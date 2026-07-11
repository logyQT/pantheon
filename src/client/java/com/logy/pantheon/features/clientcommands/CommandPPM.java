package com.logy.pantheon.features.clientcommands;

import com.logy.pantheon.utils.ChatUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class CommandPPM extends ClientCommandBase {

    @Override
    public String getName() {
        return "ppm";
    }

    @Override
    protected void build(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        builder.then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument("message", StringArgumentType.greedyString())
                .executes(context -> {
                    String input = StringArgumentType.getString(context, "message");
                    String processed = processPattern(input);

                    ChatUtils.sendPartyMessage(processed);
                    return 1;
                }));
    }

    private String processPattern(String input) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("_\\*(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(input);

        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            sb.append(input, lastEnd, matcher.start());
            try {
                int count = Integer.parseInt(matcher.group(1));
                sb.append(" ".repeat(Math.max(0, count)));
            } catch (NumberFormatException e) {
                sb.append(matcher.group(0)); // Jeśli błąd, zostaw oryginał
            }
            lastEnd = matcher.end();
        }
        sb.append(input.substring(lastEnd));

        return sb.toString();
    }
}
