package com.logy.pantheon.features.commands.main;

public interface GameInstance {
    boolean isActive();
    void update();
    void stop();
    void handleChat(String sender, String message);
}