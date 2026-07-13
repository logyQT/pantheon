package com.logy.pantheon.features.commands;

import com.logy.pantheon.features.commands.main.AutoRegister;
import com.logy.pantheon.features.commands.main.BaseCommand;
import com.logy.pantheon.utils.ChatUtils;

@AutoRegister
public class CommandAscii extends BaseCommand {

    @Override
    public String getName() { return "cat"; }

    @Override
    protected void onRun(String sender, String target, String[] args) {
        ChatUtils.sendPartyMessage("  /\\_/\\  ");
        ChatUtils.sendPartyMessage(" ( o.o ) ");
        ChatUtils.sendPartyMessage("  > ^ <  ");
        ChatUtils.sendPartyMessage(" A cat!  ");
    }
}
