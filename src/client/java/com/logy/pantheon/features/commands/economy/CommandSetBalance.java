package com.logy.pantheon.features.commands.economy;

import com.logy.pantheon.features.commands.main.AutoRegister;
import com.logy.pantheon.features.commands.main.BaseCommand;
import com.logy.pantheon.utils.ChatUtils;

@AutoRegister
public class CommandSetBalance extends BaseCommand {

    @Override
    public String getName() {
        return "setbal";
    }

    @Override
    protected void onRun(String sender, String target, String[] args) {
        if(!sender.equalsIgnoreCase("catgirlbialas")) return;
        if (args.length < 2) return;
        try {
            int amount = Integer.parseInt(args[1]);
            Economy.addMoney(target, amount);
            ChatUtils.sendPartyMessage("Balance of " + target + " set to " + amount);
        } catch (NumberFormatException e) {
            //
        }
    }
}

