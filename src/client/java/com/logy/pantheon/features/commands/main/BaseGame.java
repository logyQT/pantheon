package com.logy.pantheon.features.commands.main;

import com.logy.pantheon.utils.ChatUtils;

public abstract class BaseGame implements GameInstance {

    public enum GameState {
        IDLE,
        RUNNING,
        ENDED
    }

    public enum StopReason {
        MANUAL,
        TIMEOUT,
        ERROR,
        WIN,
        LOSS
    }

    protected GameState state = GameState.IDLE;
    protected long startTime = 0;
    protected long lastActivityTime = 0;
    protected long lastUpdateTime = 0;
    protected long stateChangeTime = 0;
    protected String starterName = null;

    @Override
    public final boolean isActive() {
        return state == GameState.RUNNING; //
    }

    protected final long now() {
        return System.currentTimeMillis();
    }

    protected boolean canStart(String sender) {
        return true;
    }

    public final void start(String sender) {
        if (state == GameState.RUNNING || !canStart(sender)) return;

        this.state = GameState.RUNNING;
        this.starterName = sender;
        this.startTime = now();
        this.lastUpdateTime = startTime;
        resetTimer();

        onStart();
    }

    public final void stop(StopReason reason) {
        if (state == GameState.IDLE) return;

        onCleanup(reason);

        this.state = GameState.IDLE;
        this.starterName = null;
    }

    @Override
    public final void stop() {
        stop(StopReason.MANUAL);
    }

    @Override
    public final void handleChat(String sender, String message) {
        if (state != GameState.RUNNING) return;

        resetTimer();
        onChat(sender, message);
    }

    @Override
    public final void update() {
        if (state != GameState.RUNNING) return;

        long currentTime = now();
        long delta = currentTime - lastUpdateTime;
        lastUpdateTime = currentTime;

        if (currentTime - lastActivityTime > getTimeoutMs()) {
            onTimeout();
            stop(StopReason.TIMEOUT);
            return;
        }

        onUpdate(delta);
    }

    protected final void resetTimer() {
        this.lastActivityTime = now();
    }

    protected final void send(String message) {
        ChatUtils.sendPartyMessage(message);
    }

    protected abstract void onStart();

    protected abstract void onChat(String sender, String message);

    protected void onUpdate(long deltaMs) {}

    protected abstract void onCleanup(StopReason reason);

    protected void onTimeout() {
        send("Game was canceled due to inactivity.");
    }

    protected long getTimeoutMs() {
        return 30000L;
    }

    protected abstract String getName();
}