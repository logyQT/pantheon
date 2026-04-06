package com.logy.pantheon.features.clientcommands;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.reflections.Reflections;
import static com.logy.pantheon.PantheonMod.LOGGER;

public class ClientCommandManager {

    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            Reflections reflections = new Reflections("com.logy.pantheon.features.clientcommands");

            reflections.getSubTypesOf(ClientCommandBase.class).forEach(clazz -> {
                try {
                    if (java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) return;

                    ClientCommandBase cmd = clazz.getDeclaredConstructor().newInstance();
                    cmd.register(dispatcher);
                    LOGGER.info("[Pantheon] Registered client command: /" + cmd.getName());
                } catch (Exception e) {
                    LOGGER.error("[Pantheon] Failed to register client command: " + clazz.getSimpleName(), e);
                }
            });
        });
    }
}
