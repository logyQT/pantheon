package com.logy.pantheon.features.clientcommands;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.reflections.Reflections;
import java.util.ArrayList;
import java.util.List;
import static com.logy.pantheon.PantheonMod.LOGGER;

public class ClientCommandManager {

    private static boolean initialized = false;
    private static final List<ClientCommandBase> COMMAND_INSTANCES = new ArrayList<>();

    public static synchronized void init() {
        if (initialized) return;

        try {
            Reflections reflections = new Reflections("com.logy.pantheon.features.clientcommands");
            reflections.getSubTypesOf(ClientCommandBase.class).forEach(clazz -> {
                try {
                    if (java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) return;
                    COMMAND_INSTANCES.add(clazz.getDeclaredConstructor().newInstance());
                } catch (Exception e) {
                    LOGGER.error("[Pantheon] Failed to create command instance: " + clazz.getSimpleName(), e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("[Pantheon] Failed to scan client commands", e);
        }

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            for (ClientCommandBase cmd : COMMAND_INSTANCES) {
                cmd.register(dispatcher);
                if (!initialized) {
                    LOGGER.info("[Pantheon] Registered client command: /" + cmd.getName());
                }
            }
            initialized = true;
        });
    }
}