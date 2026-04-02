package com.logy.pantheon.features.commands.whoami;

public record WhoAmIEntry(
        String name,
        String hintOne,
        String hintTwo,
        String definition
) {}