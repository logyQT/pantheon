package com.logy.pantheon.utils;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class TPSMonitor {

    private static final int MAX_SAMPLES = 100;

    private static final long[] msPerTickBuffer = new long[MAX_SAMPLES];
    private static int bufferHead = 0;
    private static int bufferCount = 0;

    private static long lastServerTickNano = -1;

    public static void init() {
        ClientTickEvents.END_LEVEL_TICK.register(world -> {
            long now = System.nanoTime();

            if (lastServerTickNano != -1) {
                long diffMs = (now - lastServerTickNano) / 1_000_000L;
                msPerTickBuffer[bufferHead] = diffMs;
                bufferHead = (bufferHead + 1) % MAX_SAMPLES;
                if (bufferCount < MAX_SAMPLES) bufferCount++;
            }

            lastServerTickNano = now;
        });
    }

    public static void onWorldChange() {
        lastServerTickNano = -1;
        bufferHead = 0;
        bufferCount = 0;
    }
}