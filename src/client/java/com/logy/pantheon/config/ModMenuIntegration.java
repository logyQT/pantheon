package com.logy.pantheon.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModMenuIntegration::createConfigScreen;
    }
    public static Screen createConfigScreen(Screen parent) {
        PantheonConfig config = PantheonConfig.get();
        return ModMenuIntegration::createConfigScreen;
    }
    public static Screen createConfigScreen(Screen parent) {
        PantheonConfig config = PantheonConfig.get();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Pantheon Mod Settings"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Party Games"))
                        .option(Option.<String>createBuilder()
                                .name(Component.literal("Command Prefix"))
                                .binding("!", () -> config.prefix, val -> config.prefix = val)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Message Que Cooldown MS"))
                                .binding(1000, () -> config.MESSAGE_QUE_COOLDOWN_MS, val -> config.MESSAGE_QUE_COOLDOWN_MS = val)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(100, 1000).step(10))
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Blackjack Settings"))
                                .collapsed(true)
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Min Bet"))
                                        .binding(10, () -> config.BLACKJACK_MIN_BET, val -> config.BLACKJACK_MIN_BET = val)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 1000).step(1))
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Max Bet"))
                                        .binding(1000000, () -> config.BLACKJACK_MAX_BET, val -> config.BLACKJACK_MAX_BET = val)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1000, 5000000).step(1000))
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Betting Time (s)"))
                                        .binding(11, () -> config.BLACKJACK_BETTING_TIME_S, val -> config.BLACKJACK_BETTING_TIME_S = val)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(5, 60).step(1))
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Turn Time (s)"))
                                        .binding(30, () -> config.BLACKJACK_TURN_TIME_S, val -> config.BLACKJACK_TURN_TIME_S = val)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(10, 120).step(1))
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Insurance Time (s)"))
                                        .binding(8, () -> config.BLACKJACK_INSURANCE_TIME_S, val -> config.BLACKJACK_INSURANCE_TIME_S = val)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(3, 30).step(1))
                                        .build())
                                .build())
                        .build())
                // HELPERS
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Helpers"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Fast Warp Settings"))
                                .collapsed(true)
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Player Transfer Cooldown Protection"))
                                        .description(OptionDescription.of(
                                                Component.literal("Time in milliseconds before a player can teleport again.\nSet to 0 to disable.")
                                        ))
                                        .binding(0, () -> config.FASTWARP_PTC_PROTECTION_MS, val -> config.FASTWARP_PTC_PROTECTION_MS = val)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 5000).step(100))
                                        .build())
                                .option(Option.<String>createBuilder()
                                        .name(Component.literal("First area name"))
                                        .binding("", () -> config.FASTWARP_AREA_ONE, val -> config.FASTWARP_AREA_ONE = val)
                                        .controller(StringControllerBuilder::create)
                                        .build())
                                .option(Option.<String>createBuilder()
                                        .name(Component.literal("Command bound to First Area"))
                                        .binding("", () -> config.FASTWARP_CMD_ONE, val -> config.FASTWARP_CMD_ONE = val)
                                        .controller(StringControllerBuilder::create)
                                        .build())
                                .option(Option.<String>createBuilder()
                                        .name(Component.literal("Second area name"))
                                        .binding("", () -> config.FASTWARP_AREA_TWO, val -> config.FASTWARP_AREA_TWO = val)
                                        .controller(StringControllerBuilder::create)
                                        .build())
                                .option(Option.<String>createBuilder()
                                        .name(Component.literal("Command bound to Second Area"))
                                        .binding("", () -> config.FASTWARP_CMD_TWO, val -> config.FASTWARP_CMD_TWO = val)
                                        .controller(StringControllerBuilder::create)
                                        .build())
                        .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Enable Auto Pearls"))
                                .binding(false, () -> config.AUTO_PEARLS, val -> config.AUTO_PEARLS = val)
                                .controller(BooleanControllerBuilder::create)
                                .build())
                .build())
                // AUTO EXPERIMENTS
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Auto Experiments"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Enable Auto Experiments"))
                                .binding(false, () -> config.AUTO_EXPERIMENTS, val -> config.AUTO_EXPERIMENTS = val)
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Auto Close"))
                                .binding(false, () -> config.AUTO_EXPERIMENTS_AUTO_CLOSE, val -> config.AUTO_EXPERIMENTS_AUTO_CLOSE = val)
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Get Max XP"))
                                .binding(false, () -> config.AUTO_EXPERIMENTS_GET_MAX_XP, val -> config.AUTO_EXPERIMENTS_GET_MAX_XP = val)
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Click Delay (ms)"))
                                .binding(200, () -> config.AUTO_EXPERIMENTS_CLICK_DELAY, val -> config.AUTO_EXPERIMENTS_CLICK_DELAY = val)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(100, 1000).step(10))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Serum Count"))
                                .binding(0, () -> config.AUTO_EXPERIMENTS_SERUM_COUNT, val -> config.AUTO_EXPERIMENTS_SERUM_COUNT = val)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 3).step(1))
                                .build())
                        .build())

                .save(config::save)
                .build()
                .generateScreen(parent);
                .save(config::save)
                .build()
                .generateScreen(parent);
    }
}