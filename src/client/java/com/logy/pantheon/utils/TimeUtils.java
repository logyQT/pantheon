package com.logy.pantheon.utils;

public class TimeUtils {

    public static String formatDuration(long milliseconds) {
        long totalSeconds = milliseconds / 1000;

        if (totalSeconds <= 0) return "0 seconds";

        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();

        if (minutes > 0) {
            sb.append(minutes).append(minutes == 1 ? " minute " : " minutes ");
        }

        if (seconds > 0 || minutes == 0) {
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }

        return sb.toString().trim();
    }
}
