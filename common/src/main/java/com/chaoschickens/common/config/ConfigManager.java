package com.chaoschickens.common.config;

import java.util.*;

/**
 * Platform-agnostic configuration manager.
 * Handles loading, parsing, and providing access to configuration values.
 * Supports config versioning and migration through {@link ConfigVersion}.
 * Platform-specific modules should extend this to integrate with their
 * respective config systems (YAML for Bukkit, JSON for Fabric/Forge).
 */
public class ConfigManager {

    private int configVersion = ConfigVersion.CURRENT_VERSION;
    private double chaosChance = 0.35;          // 35% chance a chicken gets a trait
    private boolean enableBossChickens = true;
    private double bossChance = 0.02;           // 2% chance for a boss chicken
    private int bossTraitCount = 3;
    private boolean enableTraitParticles = true;
    private boolean enableTraitMessages = true;
    private boolean announceTraitOnSpawn = false;
    private boolean onlyNaturalSpawns = false;   // Only apply to naturally spawned chickens
    private int maxChickensPerPlayer = -1;       // -1 = unlimited
    private boolean checkForUpdates = true;      // Check Modrinth for updates on startup
    private boolean enableFoliaSupport = true;   // Enable Folia-compatible scheduling
    private boolean bstatsEnabled = true;        // Enable bStats analytics
    private Map<String, Boolean> traitEnabled = new LinkedHashMap<>();
    private Map<String, Double> traitWeights = new LinkedHashMap<>();
    private Map<String, Object> customValues = new LinkedHashMap<>();

    public ConfigManager() {
        // Initialize default trait enabled states and weights
        String[] defaultTraits = {
                "explosive", "speed", "fire", "magnet", "golden",
                "disco", "zombie", "teleport", "ice", "cursed"
        };
        double[] defaultWeights = {
                1.0, 1.2, 1.0, 0.8, 0.5,
                1.0, 0.7, 0.6, 0.8, 0.7
        };
        for (int i = 0; i < defaultTraits.length; i++) {
            traitEnabled.put(defaultTraits[i], true);
            traitWeights.put(defaultTraits[i], defaultWeights[i]);
        }
    }

    // --- Getters ---

    public int getConfigVersion() {
        return configVersion;
    }

    public double getChaosChance() {
        return chaosChance;
    }

    public boolean isEnableBossChickens() {
        return enableBossChickens;
    }

    public double getBossChance() {
        return bossChance;
    }

    public int getBossTraitCount() {
        return bossTraitCount;
    }

    public boolean isEnableTraitParticles() {
        return enableTraitParticles;
    }

    public boolean isEnableTraitMessages() {
        return enableTraitMessages;
    }

    public boolean isAnnounceTraitOnSpawn() {
        return announceTraitOnSpawn;
    }

    public boolean isOnlyNaturalSpawns() {
        return onlyNaturalSpawns;
    }

    public int getMaxChickensPerPlayer() {
        return maxChickensPerPlayer;
    }

    public boolean isCheckForUpdates() {
        return checkForUpdates;
    }

    public boolean isEnableFoliaSupport() {
        return enableFoliaSupport;
    }

    public boolean isBstatsEnabled() {
        return bstatsEnabled;
    }

    public boolean isTraitEnabled(String traitKey) {
        return traitEnabled.getOrDefault(traitKey, false);
    }

    public double getTraitWeight(String traitKey) {
        return traitWeights.getOrDefault(traitKey, 1.0);
    }

    public Map<String, Boolean> getTraitEnabledMap() {
        return Collections.unmodifiableMap(traitEnabled);
    }

    public Map<String, Double> getTraitWeightMap() {
        return Collections.unmodifiableMap(traitWeights);
    }

    public Object getCustomValue(String key) {
        return customValues.get(key);
    }

    public <T> T getCustomValue(String key, Class<T> type, T defaultValue) {
        Object val = customValues.get(key);
        if (val == null) return defaultValue;
        try {
            return type.cast(val);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    // --- Setters (used by platform-specific config loaders) ---

    public void setConfigVersion(int configVersion) {
        this.configVersion = configVersion;
    }

    public void setChaosChance(double chaosChance) {
        this.chaosChance = Math.max(0.0, Math.min(1.0, chaosChance));
    }

    public void setEnableBossChickens(boolean enableBossChickens) {
        this.enableBossChickens = enableBossChickens;
    }

    public void setBossChance(double bossChance) {
        this.bossChance = Math.max(0.0, Math.min(1.0, bossChance));
    }

    public void setBossTraitCount(int bossTraitCount) {
        this.bossTraitCount = Math.max(1, Math.min(10, bossTraitCount));
    }

    public void setEnableTraitParticles(boolean enableTraitParticles) {
        this.enableTraitParticles = enableTraitParticles;
    }

    public void setEnableTraitMessages(boolean enableTraitMessages) {
        this.enableTraitMessages = enableTraitMessages;
    }

    public void setAnnounceTraitOnSpawn(boolean announceTraitOnSpawn) {
        this.announceTraitOnSpawn = announceTraitOnSpawn;
    }

    public void setOnlyNaturalSpawns(boolean onlyNaturalSpawns) {
        this.onlyNaturalSpawns = onlyNaturalSpawns;
    }

    public void setMaxChickensPerPlayer(int maxChickensPerPlayer) {
        this.maxChickensPerPlayer = maxChickensPerPlayer;
    }

    public void setCheckForUpdates(boolean checkForUpdates) {
        this.checkForUpdates = checkForUpdates;
    }

    public void setEnableFoliaSupport(boolean enableFoliaSupport) {
        this.enableFoliaSupport = enableFoliaSupport;
    }

    public void setBstatsEnabled(boolean bstatsEnabled) {
        this.bstatsEnabled = bstatsEnabled;
    }

    public void setTraitEnabled(String traitKey, boolean enabled) {
        traitEnabled.put(traitKey, enabled);
    }

    public void setTraitWeight(String traitKey, double weight) {
        traitWeights.put(traitKey, Math.max(0.0, weight));
    }

    public void setCustomValue(String key, Object value) {
        customValues.put(key, value);
    }

    /**
     * Apply the configuration to the ChaosChickensAPI.
     * Updates trait enabled states, weights, and bStats setting in the API.
     */
    public void applyToAPI() {
        for (Map.Entry<String, Boolean> entry : traitEnabled.entrySet()) {
            com.chaoschickens.common.trait.TraitType type =
                    com.chaoschickens.common.trait.TraitType.fromKey(entry.getKey());
            if (type != com.chaoschickens.common.trait.TraitType.EMPTY) {
                com.chaoschickens.api.ChaosChickensAPI.setTraitEnabled(type, entry.getValue());
                Double weight = traitWeights.get(entry.getKey());
                if (weight != null) {
                    com.chaoschickens.api.ChaosChickensAPI.setTraitWeight(type, weight);
                }
            }
        }
        com.chaoschickens.api.ChaosChickensAPI.setBstatsEnabled(bstatsEnabled);
    }
}
