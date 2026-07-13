package com.logy.pantheon.config;

import com.logy.pantheon.config.gui.Category;
import com.logy.pantheon.config.gui.Module;
import com.logy.pantheon.config.gui.util.ColorUtil;
import com.logy.pantheon.config.gui.widgets.BooleanWidget;
import com.logy.pantheon.config.gui.widgets.SliderWidget;
import com.logy.pantheon.config.gui.widgets.TextWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class PantheonClickGui extends Screen {

    private final List<Category> panels = new ArrayList<>();

    public PantheonClickGui(Screen parent) {
        super(Component.literal("Pantheon Config"));
        buildPanels();
    }

    private void buildPanels() {
        PantheonConfig c = PantheonConfig.get();

        // ── Main Category ─────────────────────────────────────
        Category main = new Category("Main", 10, 10);

        Module general = new Module("General");
        general.add(new TextWidget("Command Prefix", () -> c.prefix, v -> c.prefix = v));
        general.add(new SliderWidget("Message Queue Cooldown", 100, 1000, 10, () -> c.MESSAGE_QUE_COOLDOWN_MS, v -> c.MESSAGE_QUE_COOLDOWN_MS = v));
        general.add(new SliderWidget("Max Retries", 0, 10, 1, () -> c.MESSAGE_QUE_MAX_RETRIES, v -> c.MESSAGE_QUE_MAX_RETRIES = v));
        main.add(general);

        Module autoPearls = new Module("Auto Pearls", () -> c.AUTO_PEARLS, v -> c.AUTO_PEARLS = v);
        main.add(autoPearls);

        Module fastTeleport = new Module("Fast Teleport");
        fastTeleport.add(new SliderWidget("Warp PTC Protection", 0, 5000, 100, () -> c.FASTWARP_PTC_PROTECTION_MS, v -> c.FASTWARP_PTC_PROTECTION_MS = v));
        fastTeleport.add(new TextWidget("Warp Area 1", () -> c.FASTWARP_AREA_ONE, v -> c.FASTWARP_AREA_ONE = v));
        fastTeleport.add(new TextWidget("Warp Cmd 1", () -> c.FASTWARP_CMD_ONE, v -> c.FASTWARP_CMD_ONE = v));
        fastTeleport.add(new TextWidget("Warp Area 2", () -> c.FASTWARP_AREA_TWO, v -> c.FASTWARP_AREA_TWO = v));
        fastTeleport.add(new TextWidget("Warp Cmd 2", () -> c.FASTWARP_CMD_TWO, v -> c.FASTWARP_CMD_TWO = v));
        main.add(fastTeleport);

        Module autoExp = new Module("Auto Experiments", () -> c.AUTO_EXPERIMENTS, v -> c.AUTO_EXPERIMENTS = v);
        autoExp.add(new BooleanWidget("Auto Close", () -> c.AUTO_EXPERIMENTS_AUTO_CLOSE, v -> c.AUTO_EXPERIMENTS_AUTO_CLOSE = v));
        autoExp.add(new BooleanWidget("Get Max XP", () -> c.AUTO_EXPERIMENTS_GET_MAX_XP, v -> c.AUTO_EXPERIMENTS_GET_MAX_XP = v));
        autoExp.add(new SliderWidget("Click Delay (ms)", 100, 1000, 10, () -> c.AUTO_EXPERIMENTS_CLICK_DELAY, v -> c.AUTO_EXPERIMENTS_CLICK_DELAY = v));
        autoExp.add(new SliderWidget("Serum Count", 0, 3, 1, () -> c.AUTO_EXPERIMENTS_SERUM_COUNT, v -> c.AUTO_EXPERIMENTS_SERUM_COUNT = v));
        main.add(autoExp);

        panels.add(main);

        // ── Party Games Category ──────────────────────────────
        Category party = new Category("Party Games", 200, 10);

        Module blackjack = new Module("Blackjack", () -> c.BLACKJACK_ENABLED, v -> c.BLACKJACK_ENABLED = v);
        blackjack.add(new SliderWidget("Min Bet", 1, 1000, 1, () -> c.BLACKJACK_MIN_BET, v -> c.BLACKJACK_MIN_BET = v));
        blackjack.add(new SliderWidget("Max Bet", 1000, 5000000, 1000, () -> c.BLACKJACK_MAX_BET, v -> c.BLACKJACK_MAX_BET = v));
        blackjack.add(new SliderWidget("BJ Payout Mult", 10, 50, 1, () -> (int) (c.BLACKJACK_BJ_PAYOUT * 10), v -> c.BLACKJACK_BJ_PAYOUT = v / 10.0));
        blackjack.add(new SliderWidget("Insurance Mult", 10, 50, 1, () -> (int) (c.BLACKJACK_INSURANCE_PAYOUT * 10), v -> c.BLACKJACK_INSURANCE_PAYOUT = v / 10.0));
        blackjack.add(new SliderWidget("Betting Time (s)", 5, 60, 1, () -> c.BLACKJACK_BETTING_TIME_MS / 1000, v -> c.BLACKJACK_BETTING_TIME_MS = v * 1000));
        blackjack.add(new SliderWidget("Turn Time (s)", 10, 120, 1, () -> c.BLACKJACK_TURN_TIME_MS / 1000, v -> c.BLACKJACK_TURN_TIME_MS = v * 1000));
        blackjack.add(new SliderWidget("Insurance Time (s)", 3, 30, 1, () -> c.BLACKJACK_INSURANCE_TIME_MS / 1000, v -> c.BLACKJACK_INSURANCE_TIME_MS = v * 1000));
        party.add(blackjack);

        Module hangman = new Module("Hangman", () -> c.HANGMAN_ENABLED, v -> c.HANGMAN_ENABLED = v);
        hangman.add(new SliderWidget("Lives", 1, 10, 1, () -> c.HANGMAN_LIVES, v -> c.HANGMAN_LIVES = v));
        hangman.add(new SliderWidget("Timeout (s)", 10, 120, 5, () -> c.HANGMAN_TIMEOUT_MS / 1000, v -> c.HANGMAN_TIMEOUT_MS = v * 1000));
        party.add(hangman);

        Module hack = new Module("Hack Game", () -> c.HACK_ENABLED, v -> c.HACK_ENABLED = v);
        hack.add(new SliderWidget("Max Attempts", 1, 10, 1, () -> c.HACK_MAX_ATTEMPTS, v -> c.HACK_MAX_ATTEMPTS = v));
        hack.add(new SliderWidget("Reward", 10, 1000, 10, () -> c.HACK_REWARD, v -> c.HACK_REWARD = v));
        hack.add(new SliderWidget("Timeout (s)", 5, 60, 1, () -> c.HACK_TIMEOUT_MS / 1000, v -> c.HACK_TIMEOUT_MS = v * 1000));
        hack.add(new SliderWidget("Cooldown (m)", 1, 30, 1, () -> c.HACK_COOLDOWN_MS / 60000, v -> c.HACK_COOLDOWN_MS = v * 60000));
        party.add(hack);

        Module wordchain = new Module("Word Chain", () -> c.WORDCHAIN_ENABLED, v -> c.WORDCHAIN_ENABLED = v);
        wordchain.add(new SliderWidget("Reg. Fee", 0, 1000, 10, () -> c.WORDCHAIN_REGISTRATION_FEE, v -> c.WORDCHAIN_REGISTRATION_FEE = v));
        wordchain.add(new SliderWidget("Init. Timeout (s)", 5, 60, 1, () -> c.WORDCHAIN_INITIAL_TIMEOUT_MS / 1000, v -> c.WORDCHAIN_INITIAL_TIMEOUT_MS = v * 1000));
        wordchain.add(new SliderWidget("Timeout Decay %", 50, 100, 1, () -> c.WORDCHAIN_TIMEOUT_DECAY_PCT, v -> c.WORDCHAIN_TIMEOUT_DECAY_PCT = v));
        party.add(wordchain);

        Module roulette = new Module("Roulette", () -> c.ROULETTE_ENABLED, v -> c.ROULETTE_ENABLED = v);
        roulette.add(new SliderWidget("Max Bets/Player", 1, 20, 1, () -> c.ROULETTE_MAX_BETS_PER_PLAYER, v -> c.ROULETTE_MAX_BETS_PER_PLAYER = v));
        roulette.add(new SliderWidget("Betting Window (s)", 10, 120, 5, () -> c.ROULETTE_BETTING_TIME_MS / 1000, v -> c.ROULETTE_BETTING_TIME_MS = v * 1000));
        party.add(roulette);

        Module whoami = new Module("WhoAmI", () -> c.WHOAMI_ENABLED, v -> c.WHOAMI_ENABLED = v);
        whoami.add(new SliderWidget("Entry Cost", 1, 100, 1, () -> c.WHOAMI_ENTRY_COST, v -> c.WHOAMI_ENTRY_COST = v));
        whoami.add(new SliderWidget("Fast Payout", 10, 200, 5, () -> c.WHOAMI_PAYOUT_FAST, v -> c.WHOAMI_PAYOUT_FAST = v));
        whoami.add(new SliderWidget("Mid Payout", 10, 200, 5, () -> c.WHOAMI_PAYOUT_MID, v -> c.WHOAMI_PAYOUT_MID = v));
        whoami.add(new SliderWidget("Slow Payout", 10, 200, 5, () -> c.WHOAMI_PAYOUT_SLOW, v -> c.WHOAMI_PAYOUT_SLOW = v));
        whoami.add(new SliderWidget("Cooldown (m)", 1, 10, 1, () -> c.WHOAMI_COOLDOWN_MS / 60000, v -> c.WHOAMI_COOLDOWN_MS = v * 60000));
        party.add(whoami);

        Module math = new Module("Math Game", () -> c.MATH_ENABLED, v -> c.MATH_ENABLED = v);
        math.add(new SliderWidget("Reward", 1, 100, 1, () -> c.MATH_REWARD, v -> c.MATH_REWARD = v));
        math.add(new SliderWidget("Timeout (s)", 5, 60, 1, () -> c.MATH_TIMEOUT_MS / 1000, v -> c.MATH_TIMEOUT_MS = v * 1000));
        math.add(new SliderWidget("Cooldown (m)", 1, 10, 1, () -> c.MATH_COOLDOWN_MS / 60000, v -> c.MATH_COOLDOWN_MS = v * 60000));
        party.add(math);

        Module speedtype = new Module("Speed Typing", () -> c.SPEEDTYPE_ENABLED, v -> c.SPEEDTYPE_ENABLED = v);
        speedtype.add(new SliderWidget("Reward", 1, 100, 1, () -> c.SPEEDTYPE_REWARD, v -> c.SPEEDTYPE_REWARD = v));
        speedtype.add(new SliderWidget("Timeout (s)", 5, 60, 1, () -> c.SPEEDTYPE_TIMEOUT_MS / 1000, v -> c.SPEEDTYPE_TIMEOUT_MS = v * 1000));
        speedtype.add(new SliderWidget("Cooldown (m)", 1, 10, 1, () -> c.SPEEDTYPE_COOLDOWN_MS / 60000, v -> c.SPEEDTYPE_COOLDOWN_MS = v * 60000));
        party.add(speedtype);

        Module guess = new Module("Guessing Game", () -> c.GUESS_ENABLED, v -> c.GUESS_ENABLED = v);
        guess.add(new SliderWidget("Entry Cost", 1, 100, 1, () -> c.GUESS_ENTRY_COST, v -> c.GUESS_ENTRY_COST = v));
        guess.add(new SliderWidget("Payout <=3 tries", 10, 200, 5, () -> c.GUESS_PAYOUT_3, v -> c.GUESS_PAYOUT_3 = v));
        guess.add(new SliderWidget("Payout <=6 tries", 10, 200, 5, () -> c.GUESS_PAYOUT_6, v -> c.GUESS_PAYOUT_6 = v));
        guess.add(new SliderWidget("Speed Bonus <10s", 0, 50, 5, () -> c.GUESS_SPEED_BONUS_10S, v -> c.GUESS_SPEED_BONUS_10S = v));
        guess.add(new SliderWidget("Timeout (s)", 5, 60, 1, () -> c.GUESS_TIMEOUT_MS / 1000, v -> c.GUESS_TIMEOUT_MS = v * 1000));
        guess.add(new SliderWidget("Cooldown (m)", 1, 10, 1, () -> c.GUESS_COOLDOWN_MS / 60000, v -> c.GUESS_COOLDOWN_MS = v * 60000));
        party.add(guess);

        Module rguess = new Module("Reverse Guess", () -> c.RGUESS_ENABLED, v -> c.RGUESS_ENABLED = v);
        rguess.add(new SliderWidget("Max Attempts", 1, 20, 1, () -> c.RGUESS_MAX_ATTEMPTS, v -> c.RGUESS_MAX_ATTEMPTS = v));
        rguess.add(new SliderWidget("Timeout (s)", 10, 120, 5, () -> c.RGUESS_TIMEOUT_MS / 1000, v -> c.RGUESS_TIMEOUT_MS = v * 1000));
        party.add(rguess);

        Module wheel = new Module("Wheel Game", () -> c.WHEEL_ENABLED, v -> c.WHEEL_ENABLED = v);
        wheel.add(new SliderWidget("Reg. Fee", 0, 1000, 10, () -> c.WHEEL_REGISTRATION_FEE, v -> c.WHEEL_REGISTRATION_FEE = v));
        wheel.add(new SliderWidget("Vowel Cost", 100, 2000, 50, () -> c.WHEEL_VOWEL_COST, v -> c.WHEEL_VOWEL_COST = v));
        wheel.add(new SliderWidget("Phrase Bonus", 1000, 10000, 500, () -> c.WHEEL_PHRASE_BONUS, v -> c.WHEEL_PHRASE_BONUS = v));
        wheel.add(new SliderWidget("Turn Timeout (s)", 10, 120, 5, () -> c.WHEEL_TURN_TIMEOUT_MS / 1000, v -> c.WHEEL_TURN_TIMEOUT_MS = v * 1000));
        party.add(wheel);

        panels.add(party);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(g, mouseX, mouseY, deltaTicks);
        for (Category panel : panels) panel.tickDrag(mouseX, mouseY);
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), ColorUtil.rgba(0, 0, 0, 140));
        for (Category panel : panels) {
            panel.render(g, this.font, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent btn, boolean bl) {
        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).mouseClicked((int) (btn.x()), (int) (btn.y()), btn)) {
                if (panels.get(i).isDragging()) {
                    panels.add(panels.remove(i));
                }
                return true;
            }
        }
        return super.mouseClicked(btn, bl);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent btn) {
        for (Category p : panels) p.mouseReleased(btn);
        return super.mouseReleased(btn);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        for (Category p : panels) if (p.keyPressed(event)) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        for (Category p : panels) if (p.charTyped(event)) return true;
        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        PantheonConfig.get().save();
        super.onClose();
    }
}
