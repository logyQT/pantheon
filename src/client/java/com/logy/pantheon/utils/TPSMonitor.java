package com.logy.pantheon.utils;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;

public class TPSMonitor {

    private static final long WORLD_SWITCH_DELAY = 5000;
    private static final int MAX_SAMPLES = 100;

    private static final long[] msPerTickBuffer = new long[MAX_SAMPLES];
    private static int bufferHead = 0;
    private static int bufferCount = 0;

    private static long lastServerTickNano = -1;
    private static long lastWorldSwitchMs = 0;

    public static void init() {
        lastWorldSwitchMs = System.currentTimeMillis();

        ClientTickEvents.END_WORLD_TICK.register(world -> {
            long now = System.nanoTime();

            if (lastServerTickNano != -1) {
                long diffMs = (now - lastServerTickNano) / 1_000_000L;
                msPerTickBuffer[bufferHead] = diffMs;
                bufferHead = (bufferHead + 1) % MAX_SAMPLES;
                if (bufferCount < MAX_SAMPLES) bufferCount++;
            }

            lastServerTickNano = now;
        });

        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> onWorldChange());
    }

    public static Double getTps() {
        long now = System.currentTimeMillis();

        if (now - lastWorldSwitchMs < WORLD_SWITCH_DELAY) {
            return null;
        }

        if (bufferCount == 0) {
            return 0.0;
        }

        long elapsedSinceLastTickMs = (System.nanoTime() - lastServerTickNano) / 1_000_000L;
        if (elapsedSinceLastTickMs >= 1000) {
            return 0.0;
        }

        long sum = 0;

        for (int i = 0; i < bufferCount; i++) {
            sum += msPerTickBuffer[i];
        }

        double averageMspt = (double) sum / bufferCount;
        double tps = 1000.0 / averageMspt;
        return Math.max(0.0, Math.min(20.0, tps));
    }

    public static void onWorldChange() {
        bufferHead = 0;
        bufferCount = 0;
        lastServerTickNano = -1;
        lastWorldSwitchMs = System.currentTimeMillis();
    }

    public static long getRemainingWaitTime() {
        long elapsed = System.currentTimeMillis() - lastWorldSwitchMs;
        return Math.max(0, (WORLD_SWITCH_DELAY - elapsed) / 1000);
    }
}