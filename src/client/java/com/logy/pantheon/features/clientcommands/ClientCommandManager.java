package com.logy.pantheon.features.clientcommands;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import java.util.ArrayList;
import java.util.List;
import static com.logy.pantheon.PantheonMod.LOGGER;

public class ClientCommandManager {

    private static boolean initialized = false;
    private static final List<ClientCommandBase> COMMAND_INSTANCES = new ArrayList<>();

    public static synchronized void init() {
        if (initialized) return;

        COMMAND_INSTANCES.add(new CommandPantheonMenu());
        COMMAND_INSTANCES.add(new CommandPearls());
        COMMAND_INSTANCES.add(new CommandPPM());

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