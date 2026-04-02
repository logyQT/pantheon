package com.logy.pantheon.features.commands;

import com.logy.pantheon.features.commands.main.AutoRegister;
import com.logy.pantheon.features.commands.main.BaseCommand;
import com.logy.pantheon.utils.ChatUtils;
import com.logy.pantheon.utils.NumberUtils;

import java.util.HashMap;
import java.util.Map;

@AutoRegister
public class CommandGay extends BaseCommand {

    @Override
    public String getName() { return "gay"; }

    @Override
    protected void onRun(String sender, String target, String[] args) {
        IntRange range = GAY_OVERRIDES.getOrDefault(target.toLowerCase(), new IntRange(0, 100));

        int percent = NumberUtils.getRandomNumber(range.min(), range.max());
        ChatUtils.sendPartyMessage(target + " is " + percent + "% gay!");
    }

    private static final Map<String, IntRange> GAY_OVERRIDES = new HashMap<>();

    static {
        GAY_OVERRIDES.put("mleczkins", new IntRange(100, 150));
        GAY_OVERRIDES.put("pietrus96", new IntRange(80, 99));
        GAY_OVERRIDES.put("fronca", new IntRange(0, 10));
    }

    record IntRange(int min, int max) {}

}