package com.logy.pantheon.features.clientcommands;

import com.logy.pantheon.config.ModuleRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import graal.graalvm.polyglot.Value;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.*;

import static com.logy.pantheon.PantheonMod.LOGGER;

public class ClientCommandManager {

    private static boolean initialized = false;
    private static final List<ClientCommandBase> COMMAND_INSTANCES = new ArrayList<>();
    private static CommandDispatcher<FabricClientCommandSource> dispatcher;
    private static final Map<String, String> SCRIPT_COMMAND_MODULES = new HashMap<>();

    public static class ScriptCommandContext {
        private final CommandContext<FabricClientCommandSource> ctx;

        public ScriptCommandContext(CommandContext<FabricClientCommandSource> ctx) {
            this.ctx = ctx;
        }

        public Object get(String name) {
            try {
                return ctx.getArgument(name, Object.class);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        public String getSender() {
            var player = ctx.getSource().getPlayer();
            return player != null ? player.getName().getString() : "?";
        }
    }

    public static synchronized void init() {
        if (initialized) return;

        COMMAND_INSTANCES.add(new CommandPantheonMenu());
        COMMAND_INSTANCES.add(new CommandPearls());
        COMMAND_INSTANCES.add(new CommandPPM());

        ClientCommandRegistrationCallback.EVENT.register((d, registryAccess) -> {
            dispatcher = d;
            for (ClientCommandBase cmd : COMMAND_INSTANCES) {
                cmd.register(dispatcher);
                if (!initialized) {
                    LOGGER.info("[Pantheon] Registered client command: /" + cmd.getName());
                }
            }
            initialized = true;
        });
    }

    public static void registerScriptCommand(String moduleId, String invoker, String description,
                                              List<Map<String, Object>> argDefs, Value callback) {
        if (dispatcher == null) return;

        String existing = SCRIPT_COMMAND_MODULES.get(invoker);
        if (existing != null && !existing.equals(moduleId)) {
            throw new RuntimeException(
                "Client command '/" + invoker + "' already registered by '" + existing + "'");
        }

        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommands.literal(invoker);

        if (argDefs == null || argDefs.isEmpty()) {
            builder.executes(ctx -> executeScriptCommand(ctx, callback, moduleId));
        } else {
            attachExecAndChildren(builder, argDefs, 0, callback, moduleId);
        }

        dispatcher.register(builder);
        SCRIPT_COMMAND_MODULES.put(invoker, moduleId);
        LOGGER.info("[Pantheon] Registered script client command: /{} (module: {})", invoker, moduleId);
    }

    public static void unregisterScriptCommand(String invoker, String moduleId) {
        String stored = SCRIPT_COMMAND_MODULES.get(invoker);
        if (moduleId.equals(stored)) {
            SCRIPT_COMMAND_MODULES.remove(invoker);
        }
    }

    private static void attachExecAndChildren(
            ArgumentBuilder<FabricClientCommandSource, ?> node,
            List<Map<String, Object>> argDefs, int index,
            Value callback, String moduleId) {
        if (argDefs == null || index >= argDefs.size()) {
            node.executes(ctx -> executeScriptCommand(ctx, callback, moduleId));
            return;
        }

        Map<String, Object> def = argDefs.get(index);
        String name = (String) def.get("name");
        if (name == null) return;

        String type = (String) def.getOrDefault("type", "string");
        boolean optional = Boolean.TRUE.equals(def.get("optional"));

        RequiredArgumentBuilder<FabricClientCommandSource, ?> child = buildArgument(name, type);
        attachExecAndChildren(child, argDefs, index + 1, callback, moduleId);

        if (optional) {
            node.executes(ctx -> executeScriptCommand(ctx, callback, moduleId));
        }

        node.then(child);
    }

    private static RequiredArgumentBuilder<FabricClientCommandSource, ?> buildArgument(String name, String type) {
        return switch (type) {
            case "number" -> ClientCommands.argument(name, IntegerArgumentType.integer());
            case "boolean" -> ClientCommands.argument(name, BoolArgumentType.bool());
            case "greedy_string" -> ClientCommands.argument(name, StringArgumentType.greedyString());
            case "player" -> ClientCommands.argument(name, StringArgumentType.word());
            default -> ClientCommands.argument(name, StringArgumentType.word());
        };
    }

    private static int executeScriptCommand(CommandContext<FabricClientCommandSource> ctx,
                                             Value callback, String moduleId) {
        if (!ModuleRegistry.isEnabled(moduleId) || callback == null || !callback.canExecute()) return 0;
        try {
            callback.execute(new ScriptCommandContext(ctx));
        } catch (Exception e) {
            LOGGER.error("[ClientCommand] Script command error in module '{}': {}", moduleId, e.getMessage());
        }
        return 1;
    }
}
