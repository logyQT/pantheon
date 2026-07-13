package com.logy.pantheon.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public final class ScoreboardAreaMatcher {
    private ScoreboardAreaMatcher() {}

    public static boolean isInArea(String areaNameInScoreboard) {
        String needle = TextNormalizer.normalize(areaNameInScoreboard);
        if (needle.isBlank()) return false;

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) return false;

        Scoreboard scoreboard = client.level.getScoreboard();
        Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) return false;

        String haystack = normalizedSidebarText(scoreboard, sidebar);
        return haystack.contains(needle);
    }

    public static String sidebarContentsForDebug() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) return "<no client world>";

        Scoreboard scoreboard = client.level.getScoreboard();
        Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) return "<no sidebar objective>";

        StringBuilder builder = new StringBuilder();
        appendRaw(builder, sidebar.getDisplayName().getString());
        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(sidebar)) {
            if (entry == null || entry.isHidden()) continue;
            appendRaw(builder, sidebarLine(scoreboard, entry));
        }
        return builder.isEmpty() ? "<sidebar empty>" : builder.toString();
    }

    private static String normalizedSidebarText(Scoreboard scoreboard, Objective sidebar) {
        StringBuilder builder = new StringBuilder();
        appendNormalized(builder, sidebar.getDisplayName().getString());
        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(sidebar)) {
            if (entry == null || entry.isHidden()) {
                continue;
            }
            appendNormalized(builder, sidebarLine(scoreboard, entry));
        }
        return builder.toString();
    }

    private static void appendNormalized(StringBuilder builder, String rawText) {
        String normalized = TextNormalizer.normalize(rawText);
        if (normalized.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(' ');
        }
        builder.append(normalized);
    }

    private static void appendRaw(StringBuilder builder, String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(" | ");
        }
        builder.append(rawText);
    }

    private static String sidebarLine(Scoreboard scoreboard, PlayerScoreEntry entry) {
        if (entry.display() != null) {
            return entry.display().getString();
        }
        String owner = entry.owner();
        PlayerTeam team = scoreboard.getPlayersTeam(owner);
        return PlayerTeam.formatNameForTeam(team, Component.literal(owner)).getString();
    }
}