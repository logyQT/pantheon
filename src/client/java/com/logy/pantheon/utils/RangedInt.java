package com.logy.pantheon.utils;

public class RangedInt {
    private int value;
    private final int min;
    private final int max;

    public RangedInt(int initial, int min, int max){
        this.min=min;
        this.max=max;
        set(initial);
    }

    public void set (int newValue){
        this.value = Math.clamp(newValue, min, max);
    }

    public int get(){
        return value;
    }

    @Override
    public String toString(){
        return String.valueOf(value);
    }
}
