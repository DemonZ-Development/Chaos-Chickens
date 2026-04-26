/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * Lead Developer: Cyrus
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.bukkit;

import com.chaoschickens.api.ChaosChickensAPI;
import com.chaoschickens.bukkit.command.ChaosCommand;
import com.chaoschickens.bukkit.listener.ChickenDeathListener;
import com.chaoschickens.bukkit.listener.ChickenInteractListener;
import com.chaoschickens.bukkit.listener.ChickenSpawnListener;
import com.chaoschickens.bukkit.trait.*;
import com.chaoschickens.bukkit.util.ChickenDataUtil;
import com.chaoschickens.common.config.ConfigManager;
import com.chaoschickens.common.config.ConfigVersion;
import com.chaoschickens.common.trait.TraitType;
import com.chaoschickens.common.util.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Main plugin class for ChaosChickens on Bukkit/Spigot/Paper/Purpur/Folia.
 * Created and maintained by the DemonZ Development community.
 * Lead Developer: Cyrus
 *
 * Manages trait registration, chicken tracking, and periodic trait ticking.
 * Supports Folia's regionized multithreading when detected.
 */
public class ChaosChickensBukkit extends org.bukkit.plugin.java.JavaPlugin {

    private final ConfigManager configManager = new ConfigManager();
    private final Map<TraitType, BukkitTrait> traitMap = new LinkedHashMap<>();
    private final Map<UUID, TraitType> activeChickens = new ConcurrentHashMap<>(); // Bug #7 fix: ConcurrentHashMap
    private final Map<UUID, Integer> chickenTickCounters = new ConcurrentHashMap<>(); // Per-chicken tick tracking
    private final Random random = new Random(); // Bug #9 fix: shared Random instance
    private boolean isFolia = false;
    private boolean foliaTaskRunning = false;

    private BukkitTask traitTickTask;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Detect Folia
        detectFolia();

        // Load configuration
        loadConfig();

        // Initialize the API
        ChaosChickensAPI.initialize();

        // Register all traits
        registerTraits();

        // Register listeners
        registerListeners();

        // Register commands
        registerCommands();

        // Start the periodic trait ticking task
        startTraitTickTask();

        // Scan existing chickens in all loaded worlds (for reload support)
        scanExistingChickens();

        // Initialize bStats metrics
        if (configManager.isBstatsEnabled()) {
            int pluginId = 30945;
            org.bstats.bukkit.Metrics metrics = new org.bstats.bukkit.Metrics(this, pluginId);

            // Add custom charts
            metrics.addCustomChart(new org.bstats.charts.SimplePie("chaos_chance",
                () -> String.valueOf((int)(configManager.getChaosChance() * 100)) + "%"));
            metrics.addCustomChart(new org.bstats.charts.SingleLineChart("active_chickens",
                () -> activeChickens.size()));
            metrics.addCustomChart(new org.bstats.charts.SimplePie("folia_enabled",
                () -> String.valueOf(isFolia)));
            metrics.addCustomChart(new org.bstats.charts.DrilldownPie("trait_distribution",
                () -> {
                    Map<String, Integer> counts = new java.util.HashMap<>();
                    for (TraitType type : activeChickens.values()) {
                        counts.merge(type.getKey(), 1, Integer::sum);
                    }
                    Map<String, Map<String, Integer>> result = new java.util.HashMap<>();
                    for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                        Map<String, Integer> inner = new java.util.HashMap<>();
                        inner.put(entry.getKey(), entry.getValue());
                        result.put(entry.getKey(), inner);
                    }
                    return result;
                }));
            getLogger().info("bStats metrics enabled (Plugin ID: " + pluginId + ")");
        }

        // Check for updates via Modrinth
        if (configManager.isCheckForUpdates()) {
            String version = getDescription().getVersion();
            UpdateChecker.setCurrentVersion(version);
            UpdateChecker.checkForUpdates().thenAccept(available -> {
                if (available) {
                    getLogger().info(UpdateChecker.getUpdateMessage());
                    // Notify online admins
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.hasPermission("chaoschickens.admin")) {
                            player.sendMessage(org.bukkit.ChatColor.GOLD + "[ChaosChickens] " +
                                    org.bukkit.ChatColor.YELLOW + UpdateChecker.getUpdateMessage());
                        }
                    }
                }
            });
        }

        getLogger().info("ChaosChickens v" + getDescription().getVersion() + " has been enabled!");
        getLogger().info("Registered " + traitMap.size() + " traits.");
        getLogger().info("Folia support: " + (isFolia ? "ACTIVE" : "Not detected (standard Bukkit scheduling)"));
    }

    @Override
    public void onDisable() {
        // Cancel the ticking task
        if (traitTickTask != null) {
            traitTickTask.cancel();
            traitTickTask = null;
        }

        // Handle Folia task cancellation
        foliaTaskRunning = false;

        // Clear active chickens map
        activeChickens.clear();
        chickenTickCounters.clear();

        getLogger().info("ChaosChickens has been disabled!");
    }

    /**
     * Detect if the server is running Folia by checking for
     * the RegionizedServer class that Folia provides.
     */
    private void detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
            getLogger().info("Folia detected! Using region-aware scheduling.");
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }
    }

    /**
     * Check if the server is running Folia.
     *
     * @return true if Folia is detected
     */
    public boolean isFolia() {
        return isFolia;
    }

    /**
     * Get the shared Random instance for all trait operations.
     *
     * @return The shared Random instance
     */
    public Random getRandom() {
        return random;
    }

    /**
     * Load (or reload) configuration from the Bukkit config file
     * into the platform-agnostic ConfigManager.
     * Supports config version migration.
     */
    public void loadConfig() {
        reloadConfig();
        org.bukkit.configuration.file.FileConfiguration config = getConfig();

        // Config version check and migration
        int loadedVersion = config.getInt(ConfigVersion.CONFIG_VERSION_KEY, 1);
        if (ConfigVersion.needsMigration(loadedVersion)) {
            getLogger().info("Config version " + loadedVersion + " is outdated. Migrating to version " +
                    ConfigVersion.CURRENT_VERSION + "...");
            migrateConfig(loadedVersion);
        }

        configManager.setConfigVersion(ConfigVersion.CURRENT_VERSION);
        configManager.setChaosChance(config.getDouble("chaos-chance", 0.35));
        configManager.setEnableBossChickens(config.getBoolean("enable-boss-chickens", true));
        configManager.setBossChance(config.getDouble("boss-chance", 0.02));
        configManager.setBossTraitCount(config.getInt("boss-trait-count", 3));
        configManager.setEnableTraitParticles(config.getBoolean("enable-particles", true));
        configManager.setEnableTraitMessages(config.getBoolean("enable-messages", true));
        configManager.setAnnounceTraitOnSpawn(config.getBoolean("announce-trait-on-spawn", false));
        configManager.setOnlyNaturalSpawns(config.getBoolean("only-natural-spawns", false));
        configManager.setMaxChickensPerPlayer(config.getInt("max-chickens-per-player", -1));
        configManager.setCheckForUpdates(config.getBoolean("check-for-updates", true));
        configManager.setEnableFoliaSupport(config.getBoolean("enable-folia-support", true));
        configManager.setBstatsEnabled(config.getBoolean("bstats-enabled", true));

        // Load trait-specific config
        if (config.isConfigurationSection("traits")) {
            org.bukkit.configuration.ConfigurationSection traitsSection = config.getConfigurationSection("traits");
            if (traitsSection != null) {
                for (String traitKey : traitsSection.getKeys(false)) {
                    org.bukkit.configuration.ConfigurationSection traitSection =
                            traitsSection.getConfigurationSection(traitKey);
                    if (traitSection != null) {
                        boolean enabled = traitSection.getBoolean("enabled", true);
                        double weight = traitSection.getDouble("weight", 1.0);
                        configManager.setTraitEnabled(traitKey, enabled);
                        configManager.setTraitWeight(traitKey, weight);
                    }
                }
            }
        }

        // Apply config to API
        configManager.applyToAPI();
    }

    /**
     * Migrate config from an older version to the current version.
     *
     * @param fromVersion The version being migrated from
     */
    private void migrateConfig(int fromVersion) {
        org.bukkit.configuration.file.FileConfiguration config = getConfig();

        if (fromVersion < 2) {
            // Migration from v1 to v2: add new fields
            if (!config.contains("config-version")) {
                config.set("config-version", ConfigVersion.CURRENT_VERSION);
            }
            if (!config.contains("boss-trait-count")) {
                config.set("boss-trait-count", 3);
            }
            if (!config.contains("max-chickens-per-player")) {
                config.set("max-chickens-per-player", -1);
            }
            if (!config.contains("check-for-updates")) {
                config.set("check-for-updates", true);
            }
            if (!config.contains("enable-folia-support")) {
                config.set("enable-folia-support", true);
            }
            saveConfig();
            getLogger().info("Config migrated from version " + fromVersion + " to " + ConfigVersion.CURRENT_VERSION);
        }

        if (fromVersion < 3) {
            if (!config.contains("bstats-enabled")) {
                config.set("bstats-enabled", true);
            }
            saveConfig();
            getLogger().info("Config migrated to version 3 (added bstats-enabled)");
        }
    }

    /**
     * Register all Bukkit-specific trait implementations.
     */
    private void registerTraits() {
        registerTrait(new ExplosiveTrait());
        registerTrait(new SpeedTrait());
        registerTrait(new FireTrait());
        registerTrait(new MagnetTrait());
        registerTrait(new GoldenTrait());
        registerTrait(new DiscoTrait());
        registerTrait(new ZombieTrait());
        registerTrait(new TeleportTrait());
        registerTrait(new IceTrait());
        registerTrait(new CursedTrait());
        registerTrait(new BossTrait());
    }

    /**
     * Register a single BukkitTrait with the plugin and the API.
     *
     * @param trait The trait to register
     */
    private void registerTrait(BukkitTrait trait) {
        traitMap.put(trait.getType(), trait);
        // Register with the global API (which also handles weight)
        ChaosChickensAPI.registerTrait(trait, trait.getDefaultWeight());
    }

    /**
     * Register all event listeners.
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ChickenSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new ChickenDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ChickenInteractListener(this), this);
    }

    /**
     * Register the command handler.
     */
    private void registerCommands() {
        ChaosCommand commandHandler = new ChaosCommand(this);
        getCommand("chaoschickens").setExecutor(commandHandler);
        getCommand("chaoschickens").setTabCompleter(commandHandler);
    }

    /**
     * Start the main repeating task that ticks periodic traits
     * and handles proximity-based effects.
     * Uses Folia-compatible scheduling when Folia is detected.
     */
    private void startTraitTickTask() {
        if (isFolia && configManager.isEnableFoliaSupport()) {
            startFoliaTraitTickTask();
        } else {
            startBukkitTraitTickTask();
        }
    }

    /**
     * Standard Bukkit/Paper tick task.
     * Runs every 10 ticks (0.5 seconds) as a base interval.
     * Bug #2 fix: Per-chicken tick counters for correct interval calculation.
     */
    private void startBukkitTraitTickTask() {
        traitTickTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickAllChickens();
            }
        }.runTaskTimer(this, 20L, 10L);
    }

    /**
     * Folia-compatible tick task.
     * Uses reflection to access Folia's RegionScheduler API at runtime.
     * Falls back to standard Bukkit scheduling if Folia API is not available.
     */
    private void startFoliaTraitTickTask() {
        try {
            // Use reflection to access Folia's RegionScheduler API
            // This avoids compile-time dependency on Paper/Folia-specific classes
            Class<?> regionSchedulerClass = Class.forName(
                    "io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            Object regionScheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
            java.lang.reflect.Method runAtFixedRate = regionSchedulerClass.getMethod(
                    "runAtFixedRate",
                    org.bukkit.plugin.Plugin.class,
                    java.util.function.Consumer.class,
                    org.bukkit.Location.class,
                    long.class, long.class);

            // Create a consumer that cancels when foliaTaskRunning is false
            java.util.function.Consumer<Object> taskConsumer = task -> {
                if (!foliaTaskRunning) {
                    try {
                        task.getClass().getMethod("cancel").invoke(task);
                    } catch (Exception ignored) {}
                    return;
                }
                tickAllChickens();
            };

            runAtFixedRate.invoke(regionScheduler, this, taskConsumer, (org.bukkit.Location) null, 20L, 10L);
            foliaTaskRunning = true;
            getLogger().info("Folia RegionScheduler task started successfully.");
        } catch (Exception e) {
            getLogger().warning("Failed to start Folia RegionScheduler, falling back to standard scheduling: " + e.getMessage());
            startBukkitTraitTickTask();
        }
    }

    /**
     * Tick all active chaos chickens.
     * Handles periodic trait ticking and proximity-based effects.
     * Bug #2 fix: Uses per-chicken tick counters instead of global modulo.
     */
    private void tickAllChickens() {
        // Use a copy of the keys to avoid ConcurrentModificationException
        Set<UUID> chickenUUIDs = new HashSet<>(activeChickens.keySet());

        for (UUID chickenUUID : chickenUUIDs) {
            TraitType traitType = activeChickens.get(chickenUUID);
            if (traitType == null) continue;

            // Get the entity by UUID — iterate living entities for Spigot compatibility
            Entity entity = null;
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                for (Entity e : world.getEntities()) {
                    if (e.getUniqueId().equals(chickenUUID)) {
                        entity = e;
                        break;
                    }
                }
                if (entity != null) break;
            }

            // Remove if entity is gone
            if (entity == null || entity.isDead() || !(entity instanceof Chicken)) {
                activeChickens.remove(chickenUUID);
                chickenTickCounters.remove(chickenUUID);
                continue;
            }

            Chicken chicken = (Chicken) entity;

            // Increment per-chicken tick counter
            int ticks = chickenTickCounters.merge(chickenUUID, 1, Integer::sum);

            // Get the trait implementation
            BukkitTrait trait = traitMap.get(traitType);
            if (trait == null) continue;

            // Handle periodic ticking (Bug #2 fix)
            if (trait.isPeriodic()) {
                int tickInterval = trait.getTickInterval();
                // Convert: task fires every 10 game ticks, so game ticks elapsed = ticks * 10
                // Fire when game ticks elapsed is a multiple of tickInterval
                if ((ticks * 10) % Math.max(1, tickInterval) == 0) {
                    try {
                        trait.onTick(chicken);
                    } catch (Exception e) {
                        getLogger().log(Level.WARNING,
                                "Error ticking trait " + traitType.getKey() + " for chicken " + chickenUUID, e);
                    }
                }
            }

            // Handle proximity-based effects (every 20 ticks / 2 global cycles)
            double proximityRange = trait.getProximityRange();
            if (proximityRange > 0 && ticks % 2 == 0) {
                for (Entity nearbyEntity : chicken.getNearbyEntities(
                        proximityRange, proximityRange, proximityRange)) {
                    if (nearbyEntity instanceof Player) {
                        Player player = (Player) nearbyEntity;
                        try {
                            trait.onPlayerNear(chicken, player);
                        } catch (Exception e) {
                            getLogger().log(Level.WARNING,
                                    "Error in proximity check for trait " + traitType.getKey(), e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Scan all loaded worlds for existing chaos chickens on startup/reload.
     * Re-registers them in the active chickens map based on their PDC data.
     */
    private void scanExistingChickens() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Chicken) {
                    Chicken chicken = (Chicken) entity;
                    if (ChickenDataUtil.hasTrait(this, chicken)) {
                        TraitType traitType = ChickenDataUtil.getTrait(this, chicken);
                        if (traitType != TraitType.EMPTY) {
                            activeChickens.put(chicken.getUniqueId(), traitType);
                            chickenTickCounters.put(chicken.getUniqueId(), 0);
                            // Re-apply trait effects (e.g., BossTrait health boost)
                            BukkitTrait trait = traitMap.get(traitType);
                            if (trait != null) {
                                try {
                                    trait.onApply(chicken);
                                } catch (Exception e) {
                                    getLogger().log(Level.WARNING,
                                            "Error re-applying trait " + traitType.getKey() + " on scan", e);
                                }
                            }
                        }
                    }
                }
            }
        }
        getLogger().info("Found " + activeChickens.size() + " existing chaos chickens.");
    }

    /**
     * Pick a random trait type using the API's weighted selection.
     *
     * @return A random enabled TraitType
     */
    public TraitType pickRandomTrait() {
        return ChaosChickensAPI.pickRandomTrait(random);
    }

    /**
     * Assign a trait to a chicken entity.
     * Stores the trait in PDC, registers it in the active map,
     * and calls the trait's onApply method.
     *
     * @param chicken   The chicken entity
     * @param traitType The trait type to assign
     */
    public void assignTrait(Chicken chicken, TraitType traitType) {
        if (chicken == null || traitType == null || traitType == TraitType.EMPTY) return;

        // Store trait in PersistentDataContainer
        ChickenDataUtil.setTrait(this, chicken, traitType);

        // Add to active chickens map
        activeChickens.put(chicken.getUniqueId(), traitType);

        // Reset tick counter for this chicken
        chickenTickCounters.put(chicken.getUniqueId(), 0);

        // Apply the trait's effects
        BukkitTrait trait = traitMap.get(traitType);
        if (trait != null) {
            try {
                trait.onApply(chicken);
            } catch (Exception e) {
                getLogger().log(Level.WARNING,
                        "Error applying trait " + traitType.getKey(), e);
            }
        }

        // Optionally announce the trait
        if (configManager.isAnnounceTraitOnSpawn() && configManager.isEnableTraitMessages()) {
            String message = org.bukkit.ChatColor.GOLD + "[ChaosChickens] " +
                    org.bukkit.ChatColor.YELLOW + "A " + traitType.getDisplayName() +
                    org.bukkit.ChatColor.YELLOW + " has appeared!";
            for (Player player : chicken.getWorld().getPlayers()) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * Get the trait type stored on a chicken entity via PDC.
     *
     * @param chicken The chicken entity
     * @return The TraitType, or EMPTY if none
     */
    public TraitType getChickenTrait(Chicken chicken) {
        return ChickenDataUtil.getTrait(this, chicken);
    }

    /**
     * Remove an active chicken from the tracking map.
     *
     * @param uuid The chicken's entity UUID
     */
    public void removeActiveChicken(UUID uuid) {
        activeChickens.remove(uuid);
        chickenTickCounters.remove(uuid);
    }

    /**
     * Get the number of currently tracked active chaos chickens.
     *
     * @return The count of active chaos chickens
     */
    public int getActiveChickenCount() {
        return activeChickens.size();
    }

    /**
     * Count active chaos chickens owned by a specific player
     * (chickens within the player's world and nearby range).
     *
     * @param player The player to count for
     * @return The number of active chaos chickens near this player
     */
    public int getActiveChickenCountForPlayer(Player player) {
        int count = 0;
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            if (!world.equals(player.getWorld())) continue;
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Chicken && activeChickens.containsKey(entity.getUniqueId())) {
                    Chicken chicken = (Chicken) entity;
                    if (chicken.getLocation().distanceSquared(player.getLocation()) < 128 * 128) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Get the trait map for lookup by other classes.
     *
     * @return Unmodifiable map of trait types to BukkitTrait implementations
     */
    public Map<TraitType, BukkitTrait> getTraitMap() {
        return Collections.unmodifiableMap(traitMap);
    }

    /**
     * Get the configuration manager.
     *
     * @return The ConfigManager instance
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
}
