package com.logy.pantheon.features.commands.whoami;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static com.logy.pantheon.PantheonMod.LOGGER;

public class WhoAmILoader {
    private static List<WhoAmIEntry> entries = Collections.emptyList();

    public static void load() {
        Path path = FabricLoader.getInstance()
                .getModContainer("pantheon")
                .flatMap(c -> c.findPath("assets/pantheon/whoami_entries.json"))
                .orElse(null);

        if (path == null) {
            LOGGER.error("[Pantheon] whoami_entries.json not found!");
            return;
        }

        try (InputStream is = Files.newInputStream(path);
             InputStreamReader reader = new InputStreamReader(is)) {

            Type listType = new TypeToken<List<WhoAmIEntry>>(){}.getType();
            entries = new Gson().fromJson(reader, listType);
            LOGGER.info("[Pantheon] Loaded " + entries.size() + " WhoAmI entries.");

        } catch (Exception e) {
            LOGGER.error("[Pantheon] Failed to load whoami_entries.json", e);
        }
    }

    public static List<WhoAmIEntry> getEntries() {
        return entries;
    }
}