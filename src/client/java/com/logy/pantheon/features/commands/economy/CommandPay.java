package com.logy.pantheon.features.commands.economy;

import com.logy.pantheon.features.commands.main.AutoRegister;
import com.logy.pantheon.features.commands.main.BaseCommand;
import com.logy.pantheon.utils.ChatUtils;

@AutoRegister
public class CommandPay extends BaseCommand {

    @Override
    public String getName() {
        return "pay";
    }

    @Override
    protected void onRun(String sender, String target, String[] args) {
        // !pay <player> <amount>  →  args[0] = player, args[1] = amount
        if (args.length < 2) {
            ChatUtils.sendPartyMessage("Usage: !pay <player> <amount>");
            return;
        }

        // args[0] is already resolved to `target` by BaseCommand
        if (sender.equalsIgnoreCase(target)) {
            ChatUtils.sendPartyMessage("You cannot pay yourself.");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            ChatUtils.sendPartyMessage("Amount must be a whole number.");
            return;
        }

        if (amount <= 0) {
            ChatUtils.sendPartyMessage("Amount must be greater than zero.");
            return;
        }

        if (!Economy.playerExists(target)) {
            ChatUtils.sendPartyMessage("Player '" + target + "' not found.");
            return;
        }

        if (!Economy.takeMoney(sender, amount)) {
            ChatUtils.sendPartyMessage(
                    "Insufficient funds. Your balance: " + Economy.getCurrentBalance(sender)
            );
            return;
        }

        Economy.addMoney(target, amount);
        ChatUtils.sendPartyMessage(
                sender + " paid " + target + " " + amount +
                        " coins | Your balance: " + Economy.getCurrentBalance(sender)
        );
    }
}
