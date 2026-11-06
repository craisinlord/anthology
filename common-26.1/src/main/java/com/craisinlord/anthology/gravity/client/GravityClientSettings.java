package com.craisinlord.anthology.gravity.client;

import com.craisinlord.anthology.Anthology;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;

public final class GravityClientSettings {
    private static final String SMOOTH_TRANSITIONS_KEY = "smoothGravityTransitions";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config")
            .resolve(Anthology.MODID)
            .resolve("client.json");
    private static final OptionInstance<Boolean> SMOOTH_TRANSITIONS = OptionInstance.createBoolean(
            "options.anthology.smoothGravityTransitions",
            OptionInstance.cachedConstantTooltip(Component.translatable("options.anthology.smoothGravityTransitions.tooltip")),
            load(),
            GravityClientSettings::save
    );

    private GravityClientSettings() {
    }

    public static OptionInstance<Boolean> smoothTransitionsOption() {
        return SMOOTH_TRANSITIONS;
    }

    public static boolean smoothTransitions() {
        return SMOOTH_TRANSITIONS.get();
    }

    private static boolean load() {
        if (!Files.exists(CONFIG_PATH)) {
            return false;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root.isJsonObject() && root.getAsJsonObject().has(SMOOTH_TRANSITIONS_KEY)) {
                return root.getAsJsonObject().get(SMOOTH_TRANSITIONS_KEY).getAsBoolean();
            }
        } catch (IOException | JsonParseException | IllegalStateException | ClassCastException e) {
            Anthology.LOGGER.warn("Failed to read Anthology client settings at {}, using defaults", CONFIG_PATH, e);
        }
        return false;
    }

    private static void save(boolean smoothTransitions) {
        JsonObject root = new JsonObject();
        root.addProperty(SMOOTH_TRANSITIONS_KEY, smoothTransitions);
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            Anthology.LOGGER.warn("Failed to write Anthology client settings at {}", CONFIG_PATH, e);
        }
    }
}
