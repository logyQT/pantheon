package com.logy.pantheon.features.commands.main;

import com.logy.pantheon.utils.ChatUtils;

public abstract class BaseGame implements GameInstance {
    protected boolean active = false;
    protected long startTime = 0;
    protected long lastActivityTime = 0;

    protected void internalStart() {
        this.active = true;
        this.startTime = System.currentTimeMillis();
        resetActivityTimer();
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void stop() {
        this.active = false;
        onStop(); // Optional hook for specific cleanup
    }

    protected void resetActivityTimer() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    protected boolean isTimedOut(long timeoutMs) {
        return System.currentTimeMillis() - lastActivityTime > timeoutMs;
    }

    protected void handleTimeout(String message) {
        if (active && isTimedOut(getTimeoutLimit())) {
            ChatUtils.sendPartyMessage(message);
            stop();
        }
    }

    protected long getTimeoutLimit() {
        return 30000L;
    }

    protected void onStop() {}

    @Override
    public abstract void update();

    @Override
    public abstract void handleChat(String sender, String message);
}