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
package com.chaoschickens.fabric;

import com.chaoschickens.api.ChaosChickensAPI;
import com.chaoschickens.common.trait.TraitType;
import com.chaoschickens.common.util.UpdateChecker;
import com.chaoschickens.fabric.trait.*;
import com.chaoschickens.fabric.util.ChickenDataUtil;
import com.chaoschickens.fabric.util.ConfigLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main entry point for the Chaos Chickens Fabric mod.
 * Created and maintained by the DemonZ Development community.
 * Lead Developer: Cyrus
 *
 * Handles initialization, trait registration, and the server tick loop
 * for periodic trait processing.
 */
public class ChaosChickensFabric implements ModInitializer {

    public static final String MOD_ID = "chaoschickens";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Active chaos chickens tracked by entity UUID -> trait type. */
    private static final Map<UUID, TraitType> activeChickens = new ConcurrentHashMap<>();

    /** Tracks which chickens have already had onApply called (prevents re-execution on chunk reload). */
    private static final Set<UUID> appliedChickens = ConcurrentHashMap.newKeySet();

    /** Per-chicken tick counters for correct interval calculation. */
    private static final Map<UUID, Integer> chickenTickCounters = new ConcurrentHashMap<>();

    /** Global tick counter for throttling server tick processing. */
    private static int globalTickCounter = 0;

    /** Cached trait instances for quick lookup. */
    private static final Map<TraitType, FabricTrait> traitInstances = new ConcurrentHashMap<>();

    /** Random instance for trait selection. */
    private static final Random RANDOM = new Random();

    @Override
    public void onInitialize() {
        LOGGER.info("Chaos Chickens initializing...");

        // Load configuration
        ConfigLoader.load();

        // Initialize API
        ChaosChickensAPI.initialize();

        // Register all traits
        registerTraits();

        // Register entity join event — assign traits to newly spawned chickens
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ChickenEntity chicken) {
                // Check if chicken already has a trait (e.g., from NBT on reload)
                if (ChickenDataUtil.hasTrait(chicken)) {
                    TraitType existing = ChickenDataUtil.getTrait(chicken);
                    if (existing != TraitType.EMPTY) {
                        activeChickens.put(chicken.getUuid(), existing);
                        chickenTickCounters.put(chicken.getUuid(), 0);
                        // Only re-apply trait effects if not already applied this session
                        if (appliedChickens.add(chicken.getUuid())) {
                            FabricTrait trait = getTraitInstance(existing);
                            if (trait != null) {
                                trait.onApply(chicken);
                            }
                        }
                    }
                    return;
                }

                // Roll for chaos chance
                double chaosChance = ConfigLoader.getConfig().getChaosChance();
                if (RANDOM.nextDouble() < chaosChance) {
                    assignTrait(chicken);
                }
            }
        });

        // Register server tick event — handle periodic traits
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        // Check for updates via Modrinth
        if (ConfigLoader.getConfig().isCheckForUpdates()) {
            UpdateChecker.setCurrentVersion("1.0.0");
            UpdateChecker.checkForUpdates().thenAccept(available -> {
                if (available) {
                    LOGGER.info(UpdateChecker.getUpdateMessage());
                }
            });
        }

        LOGGER.info("Chaos Chickens v1.0.0 initialized! {} traits registered.", traitInstances.size());
    }

    /**
     * Register all default chaos traits with the API.
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
     * Register a single trait instance with both the API and local cache.
     */
    private void registerTrait(FabricTrait trait) {
        ChaosChickensAPI.registerTrait(trait, trait.getDefaultWeight());
        traitInstances.put(trait.getType(), trait);
    }

    /**
     * Assign a random chaos trait to a chicken entity.
     *
     * @param chicken The chicken to assign a trait to
     */
    public static void assignTrait(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;

        TraitType traitType = ChaosChickensAPI.pickRandomTrait(RANDOM);
        if (traitType == TraitType.EMPTY) return;

        // Check for boss chicken
        if (ConfigLoader.getConfig().isEnableBossChickens()
                && RANDOM.nextDouble() < ConfigLoader.getConfig().getBossChance()) {
            traitType = TraitType.BOSS;
        }

        // Set trait on the chicken
        ChickenDataUtil.setTrait(chicken, traitType);
        activeChickens.put(chicken.getUuid(), traitType);

        // Apply trait effects
        FabricTrait trait = getTraitInstance(traitType);
        if (trait != null) {
            trait.onApply(chicken);
        }

        LOGGER.debug("Assigned trait {} to chicken at {}", traitType.getKey(), chicken.getBlockPos());
    }

    /**
     * Get the FabricTrait instance for a given trait type.
     *
     * @param type The trait type
     * @return The trait instance, or null if not registered
     */
    public static FabricTrait getTraitInstance(TraitType type) {
        return traitInstances.get(type);
    }

    /**
     * Get the trait type for an active chaos chicken.
     *
     * @param chicken The chicken entity
     * @return Optional containing the trait type
     */
    public static Optional<TraitType> getActiveTrait(ChickenEntity chicken) {
        if (chicken == null) return Optional.empty();
        return Optional.ofNullable(activeChickens.get(chicken.getUuid()));
    }

    /**
     * Remove a chicken from the active tracking map.
     *
     * @param uuid The chicken's entity UUID
     */
    public static void removeActiveChicken(UUID uuid) {
        activeChickens.remove(uuid);
        chickenTickCounters.remove(uuid);
        appliedChickens.remove(uuid);
    }

    /**
     * Server tick handler — processes periodic trait behaviors.
     * Throttled to run every 10 ticks (matching Bukkit behavior) for performance.
     * Uses per-chicken tick counters for correct interval calculation.
     */
    private void onServerTick(MinecraftServer server) {
        globalTickCounter++;
        // Only process every 10 ticks (0.5 seconds) for performance
        if (globalTickCounter % 10 != 0) return;

        for (ServerWorld world : server.getWorlds()) {
            // Get all chicken entities in the world
            List<ChickenEntity> chickens = world.getEntitiesByType(
                    EntityType.CHICKEN,
                    chicken -> activeChickens.containsKey(chicken.getUuid())
            );

            for (ChickenEntity chicken : chickens) {
                if (chicken.isRemoved() || chicken.isDead()) {
                    activeChickens.remove(chicken.getUuid());
                    chickenTickCounters.remove(chicken.getUuid());
                    appliedChickens.remove(chicken.getUuid());
                    continue;
                }

                TraitType traitType = activeChickens.get(chicken.getUuid());
                if (traitType == null) continue;

                // Increment per-chicken tick counter
                int ticks = chickenTickCounters.merge(chicken.getUuid(), 1, Integer::sum);

                FabricTrait trait = traitInstances.get(traitType);
                if (trait == null) continue;

                // Handle periodic tick using per-chicken counters
                if (trait.isPeriodic()) {
                    int tickInterval = trait.getTickInterval();
                    // Task fires every 10 game ticks, so game ticks elapsed = ticks * 10
                    if ((ticks * 10) % Math.max(1, tickInterval) == 0) {
                        try {
                            trait.onTick(chicken);
                        } catch (Exception e) {
                            LOGGER.error("Error ticking trait {} for chicken {}", traitType.getKey(), chicken.getUuid(), e);
                        }
                    }
                }

                // Handle proximity-based effects (every 2 global cycles = 20 game ticks)
                double range = trait.getProximityRange();
                if (range > 0 && ticks % 2 == 0) {
                    world.getPlayers().stream()
                            .filter(player -> !player.isDead())
                            .filter(player -> player.squaredDistanceTo(chicken) <= range * range)
                            .forEach(player -> {
                                try {
                                    trait.onPlayerNear(chicken, player);
                                } catch (Exception e) {
                                    LOGGER.error("Error in proximity check for trait {}", traitType.getKey(), e);
                                }
                            });
                }
            }
        }
    }

    /**
     * Get all active chaos chicken UUIDs. Used by mixins.
     *
     * @return Unmodifiable set of active chicken UUIDs
     */
    public static Set<UUID> getActiveChickenUUIDs() {
        return Collections.unmodifiableSet(activeChickens.keySet());
    }

    /**
     * Add a chicken to the active tracking map. Used by mixins on NBT load.
     *
     * @param uuid The chicken's entity UUID
     * @param type The trait type
     */
    public static void addActiveChicken(UUID uuid, TraitType type) {
        activeChickens.put(uuid, type);
        chickenTickCounters.put(uuid, 0);
        appliedChickens.add(uuid);
    }
}
