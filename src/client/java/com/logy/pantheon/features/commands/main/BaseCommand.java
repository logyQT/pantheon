package com.logy.pantheon.features.commands.main;

import com.logy.pantheon.utils.ChatUtils;

public abstract class BaseCommand implements ICommand {
    private static final int MAX_NAME_LENGTH = 16;

    @Override
    public void execute(String sender, String[] args) {
        String target = (args.length > 0) ? args[0] : sender;
        if (target.length() > MAX_NAME_LENGTH) {
            ChatUtils.sendPartyMessage("Error: Target name is too long");
            return;
        }
        onRun(sender, target, args);
    }

    protected abstract void onRun(String sender, String target, String[] args);
}
