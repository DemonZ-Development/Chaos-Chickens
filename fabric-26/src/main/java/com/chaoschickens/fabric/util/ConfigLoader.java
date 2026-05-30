/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.fabric.util;

import com.chaoschickens.common.config.ConfigManager;
import com.chaoschickens.common.config.ConfigVersion;
import com.chaoschickens.common.trait.TraitType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * JSON-based configuration loader for the Fabric module.
 * Loads configuration from config/chaoschickens.json in the game directory.
 * Saves a default config file if one does not exist.
 *
 * Bug #4 fix: Uses Gson.fromJson() instead of deprecated JsonParser.parseReader().
 * Bug #11 fix: Added try-catch blocks with fallback to default values for type validation.
 * Supports config versioning and migration via ConfigVersion.
 */
public final class ConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger("ChaosChickens/ConfigLoader");
    private static final String CONFIG_FILE_NAME = "chaoschickens.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ConfigManager configManager;

    private ConfigLoader() {
        // Utility class
    }

    /**
     * Get the current ConfigManager instance.
     *
     * @return The active ConfigManager
     */
    public static ConfigManager getConfig() {
        if (configManager == null) {
            configManager = new ConfigManager();
        }
        return configManager;
    }

    /**
     * Load the configuration from disk.
     * Creates a default config file if none exists.
     * Performs config migration if the version is outdated.
     */
    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);

        if (!Files.exists(configPath)) {
            LOGGER.info("No config file found, creating default at {}", configPath);
            saveDefault(configPath);
        }

        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            // Bug #4 fix: Use Gson.fromJson() instead of deprecated JsonParser.parseReader()
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root != null) {
                int loadedVersion = getSafeInt(root, ConfigVersion.CONFIG_VERSION_KEY, 1);
                if (ConfigVersion.needsMigration(loadedVersion)) {
                    LOGGER.info("Config version {} is outdated, migrating to version {}...",
                            loadedVersion, ConfigVersion.CURRENT_VERSION);
                    migrateConfig(root, configPath);
                }
                parseConfig(root);
                LOGGER.info("Configuration loaded successfully from {} (v{})", configPath,
                        configManager.getConfigVersion());
            } else {
                LOGGER.warn("Invalid config format, using defaults");
                configManager = new ConfigManager();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load config, using defaults", e);
            configManager = new ConfigManager();
        } catch (JsonSyntaxException e) {
            LOGGER.error("Invalid JSON syntax in config, using defaults", e);
            configManager = new ConfigManager();
        }

        // Apply loaded config to the API
        getConfig().applyToAPI();
    }

    /**
     * Reload the configuration from disk.
     */
    public static void reload() {
        LOGGER.info("Reloading Chaos Chickens configuration...");
        configManager = new ConfigManager();
        load();
    }

    private static void migrateConfig(JsonObject root, Path configPath) {
        root.addProperty(ConfigVersion.CONFIG_VERSION_KEY, ConfigVersion.CURRENT_VERSION);

        Map<String, Object> defaults = new java.util.LinkedHashMap<>();
        defaults.put("chaosChance", 0.50);
        defaults.put("enableBossChickens", true);
        defaults.put("bossChance", 0.02);
        defaults.put("bossTraitCount", 3);
        defaults.put("enableTraitParticles", true);
        defaults.put("enableTraitMessages", true);
        defaults.put("announceTraitOnSpawn", false);
        defaults.put("onlyNaturalSpawns", false);
        defaults.put("maxChickensPerPlayer", -1);
        defaults.put("checkForUpdates", true);
        defaults.put("enableFoliaSupport", true);
        defaults.put("bstatsEnabled", true);
        defaults.put("explosiveChickenDamageBlocks", true);

        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            if (!root.has(entry.getKey())) {
                if (entry.getValue() instanceof Boolean) {
                    root.addProperty(entry.getKey(), (Boolean) entry.getValue());
                } else if (entry.getValue() instanceof Number) {
                    root.addProperty(entry.getKey(), (Number) entry.getValue());
                }
            }
        }

        JsonObject traitsObj;
        if (root.has("traits") && root.get("traits").isJsonObject()) {
            traitsObj = root.getAsJsonObject("traits");
        } else {
            traitsObj = new JsonObject();
            root.add("traits", traitsObj);
        }

        String[] defaultTraits = {
                "explosive", "speed", "fire", "magnet", "golden",
                "disco", "zombie", "teleport", "ice", "cursed"
        };
        boolean[] defaultEnabled = {
                true, true, true, true, true,
                true, true, true, true, true
        };
        double[] defaultWeights = {
                1.0, 1.2, 1.0, 0.8, 0.5,
                1.0, 0.7, 0.6, 0.8, 0.7
        };

        for (int i = 0; i < defaultTraits.length; i++) {
            String traitKey = defaultTraits[i];
            JsonObject trait;
            if (traitsObj.has(traitKey) && traitsObj.get(traitKey).isJsonObject()) {
                trait = traitsObj.getAsJsonObject(traitKey);
            } else {
                trait = new JsonObject();
                traitsObj.add(traitKey, trait);
            }
            if (!trait.has("enabled")) {
                trait.addProperty("enabled", defaultEnabled[i]);
            }
            if (!trait.has("weight")) {
                trait.addProperty("weight", defaultWeights[i]);
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to write migrated config", e);
        }
    }

    /**
     * Save the default configuration to disk.
     *
     * @param configPath The path to write the default config
     */
    private static void saveDefault(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            LOGGER.error("Failed to create config directory", e);
            return;
        }

        JsonObject root = new JsonObject();
        root.addProperty(ConfigVersion.CONFIG_VERSION_KEY, ConfigVersion.CURRENT_VERSION);
        root.addProperty("chaosChance", 0.50);
        root.addProperty("enableBossChickens", true);
        root.addProperty("bossChance", 0.02);
        root.addProperty("bossTraitCount", 3);
        root.addProperty("enableTraitParticles", true);
        root.addProperty("enableTraitMessages", true);
        root.addProperty("announceTraitOnSpawn", false);
        root.addProperty("onlyNaturalSpawns", false);
        root.addProperty("maxChickensPerPlayer", -1);
        root.addProperty("checkForUpdates", true);
        root.addProperty("enableFoliaSupport", true);
        root.addProperty("bstatsEnabled", true);
        root.addProperty("explosiveChickenDamageBlocks", true);

        JsonObject traits = new JsonObject();
        String[] defaultTraits = {
                "explosive", "speed", "fire", "magnet", "golden",
                "disco", "zombie", "teleport", "ice", "cursed"
        };
        boolean[] defaultEnabled = {
                true, true, true, true, true,
                true, true, true, true, true
        };
        double[] defaultWeights = {
                1.0, 1.2, 1.0, 0.8, 0.5,
                1.0, 0.7, 0.6, 0.8, 0.7
        };

        for (int i = 0; i < defaultTraits.length; i++) {
            JsonObject traitObj = new JsonObject();
            traitObj.addProperty("enabled", defaultEnabled[i]);
            traitObj.addProperty("weight", defaultWeights[i]);
            traits.add(defaultTraits[i], traitObj);
        }
        root.add("traits", traits);

        try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to write default config", e);
        }
    }

    /**
     * Parse a JSON object into the ConfigManager.
     * Bug #11 fix: All getAsXxx calls wrapped in try-catch with defaults.
     *
     * @param root The root JSON object
     */
    private static void parseConfig(JsonObject root) {
        configManager = new ConfigManager();

        configManager.setConfigVersion(getSafeInt(root, ConfigVersion.CONFIG_VERSION_KEY,
                ConfigVersion.CURRENT_VERSION));
        configManager.setChaosChance(getSafeDouble(root, "chaosChance", 0.50));
        configManager.setBossChickensEnabled(getSafeBoolean(root, "enableBossChickens", true));
        configManager.setBossChance(getSafeDouble(root, "bossChance", 0.02));
        configManager.setBossTraitCount(getSafeInt(root, "bossTraitCount", 3));
        configManager.setTraitParticlesEnabled(getSafeBoolean(root, "enableTraitParticles", true));
        configManager.setTraitMessagesEnabled(getSafeBoolean(root, "enableTraitMessages", true));
        configManager.setAnnounceTraitOnSpawn(getSafeBoolean(root, "announceTraitOnSpawn", false));
        configManager.setOnlyNaturalSpawns(getSafeBoolean(root, "onlyNaturalSpawns", false));
        configManager.setMaxChickensPerPlayer(getSafeInt(root, "maxChickensPerPlayer", -1));
        configManager.setUpdateCheckingEnabled(getSafeBoolean(root, "checkForUpdates", true));
        configManager.setFoliaSupportEnabled(getSafeBoolean(root, "enableFoliaSupport", true));
        configManager.setBstatsEnabled(getSafeBoolean(root, "bstatsEnabled", true));
        configManager.setExplosiveChickenDamageBlocks(getSafeBoolean(root, "explosiveChickenDamageBlocks", true));

        // Parse trait settings
        if (root.has("traits") && root.get("traits").isJsonObject()) {
            JsonObject traits = root.getAsJsonObject("traits");
            for (Map.Entry<String, JsonElement> entry : traits.entrySet()) {
                String key = entry.getKey();
                if (entry.getValue().isJsonObject()) {
                    JsonObject traitObj = entry.getValue().getAsJsonObject();
                    configManager.setTraitEnabled(key, getSafeBoolean(traitObj, "enabled", true));
                    configManager.setTraitWeight(key, getSafeDouble(traitObj, "weight", 1.0));
                }
            }
        }
    }

    // --- Safe getters with type validation and fallbacks (Bug #11 fix) ---

    private static double getSafeDouble(JsonObject obj, String key, double defaultVal) {
        if (!obj.has(key)) return defaultVal;
        try {
            JsonElement element = obj.get(key);
            if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                return element.getAsDouble();
            }
        } catch (ClassCastException | IllegalStateException e) {
            LOGGER.warn("Invalid type for config key '{}', using default: {}", key, defaultVal);
        }
        return defaultVal;
    }

    private static int getSafeInt(JsonObject obj, String key, int defaultVal) {
        if (!obj.has(key)) return defaultVal;
        try {
            JsonElement element = obj.get(key);
            if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt();
            }
        } catch (ClassCastException | IllegalStateException e) {
            LOGGER.warn("Invalid type for config key '{}', using default: {}", key, defaultVal);
        }
        return defaultVal;
    }

    private static boolean getSafeBoolean(JsonObject obj, String key, boolean defaultVal) {
        if (!obj.has(key)) return defaultVal;
        try {
            JsonElement element = obj.get(key);
            if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
                return element.getAsBoolean();
            }
        } catch (ClassCastException | IllegalStateException e) {
            LOGGER.warn("Invalid type for config key '{}', using default: {}", key, defaultVal);
        }
        return defaultVal;
    }
}
