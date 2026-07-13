package com.logy.pantheon.features.commands.ascii;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PictureCommandLoader {

    private static final File DATA_FILE = FabricLoader.getInstance().getConfigDir().resolve("pantheon/picture-commands.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Map<String, String> load() {
        if (!DATA_FILE.exists()) createDefaults();
        try (FileReader reader = new FileReader(DATA_FILE)) {
            PictureData data = GSON.fromJson(reader, PictureData.class);
            if (data == null || data.pictures == null) return Map.of();
            return data.pictures.stream()
                .filter(p -> p.name != null && p.picture != null)
                .collect(Collectors.toMap(p -> p.name, p -> p.picture));
        } catch (IOException e) {
            e.printStackTrace();
            return Map.of();
        }
    }

    private static void createDefaults() {
        DATA_FILE.getParentFile().mkdirs();
        PictureData data = new PictureData();
        data.pictures = List.of(
            new PictureData.Picture("cat", "  /\\_/\\\n ( o.o )\n  > ^ <\n A cat!")
        );
        try (FileWriter writer = new FileWriter(DATA_FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class PictureData {
        List<Picture> pictures;
        static class Picture {
            String name;
            String picture;
            Picture() {}
            Picture(String name, String picture) {
                this.name = name;
                this.picture = picture;
            }
        }
    }
}
