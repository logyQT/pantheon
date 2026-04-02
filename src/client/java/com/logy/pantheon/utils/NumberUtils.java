package com.logy.pantheon.utils;

import java.util.concurrent.ThreadLocalRandom;

public class NumberUtils {
    public static int getRandomNumber(int min, int max){
        if(min>max) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
    public static int getClamped(int value, int min, int max){
        return Math.clamp(value, min, max);
    }
    public static int getClamped(Integer value, int min, int max, int defaultValue) {
        if (value == null) return defaultValue;
        return Math.clamp(value, min, max);
    }
}
