package com.logy.pantheon.features.commands.economy;

import com.logy.pantheon.features.commands.main.AutoRegister;
import com.logy.pantheon.features.commands.main.BaseCommand;
import com.logy.pantheon.utils.ChatUtils;

@AutoRegister
public class CommandBalance extends BaseCommand {

    @Override
    public String getName() {
        return "bal";
    }

    @Override
    protected void onRun(String sender, String target, String[] args) {
        ChatUtils.sendPartyMessage(target + " balance: " + Economy.getCurrentBalance(target));
    }
}
