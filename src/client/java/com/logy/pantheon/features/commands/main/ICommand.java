package com.logy.pantheon.features.commands.main;

public interface ICommand {
    String getName();
    void execute(String sender, String[] args);
}
