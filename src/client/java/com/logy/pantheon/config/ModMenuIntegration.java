package com.logy.pantheon.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.network.chat.Component;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            PantheonConfig config = PantheonConfig.get();

            return YetAnotherConfigLib.createBuilder()
                    .title(Component.literal("Pantheon Mod Settings"))
                    .category(ConfigCategory.createBuilder()
                            .name(Component.literal("General"))
                            .option(Option.<Boolean>createBuilder()
                                    .name(Component.literal("Show TPS Overlay"))
                                    .description(OptionDescription.of(Component.literal("Enable/Disable SkyHanni-like TPS monitor.")))
                                    .binding(true, () -> config.tpsDisplay, val -> config.tpsDisplay = val)
                                    .controller(BooleanControllerBuilder::create)
                                    .build())
                            .option(Option.<Integer>createBuilder()
                                    .name(Component.literal("Max Roulette Bet"))
                                    .binding(5000, () -> config.rouletteMaxBet, val -> config.rouletteMaxBet = val)
                                    .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(100, 100000).step(100))
                                    .build())
                            .option(Option.<String>createBuilder()
                                    .name(Component.literal("Command Prefix"))
                                    .binding("!", () -> config.prefix, val -> config.prefix = val)
                                    .controller(StringControllerBuilder::create)
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
        };
    }
}