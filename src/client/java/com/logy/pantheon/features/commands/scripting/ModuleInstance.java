package com.logy.pantheon.features.commands.scripting;

import com.logy.pantheon.config.ModuleRegistry;
import com.logy.pantheon.features.commands.main.GameInstance;
import com.logy.pantheon.utils.ChatUtils;

import static com.logy.pantheon.PantheonMod.LOGGER;

public class ModuleInstance implements GameInstance {

    private final String name;
    private final ScriptApi api;
    private boolean active = false;

    public ModuleInstance(String name, ScriptApi api) {
        this.name = name;
        this.api = api;
    }

    public ScriptApi getApi() {
        return api;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void update() {
        if (!active) return;
        if (!ModuleRegistry.isEnabled(name)) {
            stopForDisabled();
            return;
        }
        api.tickTimers();
        if (api.hasCallback("onTick")) {
            try {
                api.invokeCallback("onTick");
            } catch (Exception e) {
                LOGGER.error("[Module] onTick error in {}", name, e);
            }
        }
    }

    @Override
    public void stop() {
        if (!active) return;
        active = false;
        try {
            api.invokeCallback("onStop", "MANUAL");
        } catch (Exception e) {
            LOGGER.error("[Module] onStop error in {}", name, e);
        }
    }

    @Override
    public void handleChat(String sender, String message) {
        if (!active) return;
        try {
            api.invokeCallback("onChat", sender, message);
        } catch (Exception e) {
            LOGGER.error("[Module] onChat error in {}", name, e);
        }
    }

    public void start(String starter) {
        if (active) return;
        active = true;
        api.setStopCallback(this::stop);
        try {
            api.invokeCallback("onStart", starter);
        } catch (Exception e) {
            LOGGER.error("[Module] onStart error in {}", name, e);
            active = false;
        }
    }

    void stopForReload() {
        if (!active) return;
        active = false;
        try {
            api.invokeCallback("onStop", "RELOAD");
        } catch (Exception e) {
            LOGGER.error("[Module] onStop(RELOAD) error in {}", name, e);
        }
    }

    public void stopForDisconnect() {
        if (!active) return;
        active = false;
        try {
            api.invokeCallback("onStop", "DISCONNECT");
        } catch (Exception e) {
            LOGGER.error("[Module] onStop(DISCONNECT) error in {}", name, e);
        }
    }

    void stopForDisabled() {
        if (!active) return;
        active = false;
        String displayName = api.getDisplayName();
        ChatUtils.sendPartyMessage((displayName != null ? displayName : name) + " was stopped (disabled).");
        try {
            api.invokeCallback("onStop", "DISABLED");
        } catch (Exception e) {
            LOGGER.error("[Module] onStop(DISABLED) error in {}", name, e);
        }
    }

    public String getName() {
        return name;
    }
}
