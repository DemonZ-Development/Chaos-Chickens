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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */
package com.chaoschickens.api;

import com.chaoschickens.common.trait.ChaosTrait;
import com.chaoschickens.common.trait.TraitType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * Public API for the Chaos Chickens plugin/mod.
 * Created and maintained by the DemonZ Development community.
 * Lead Developer: Cyrus
 *
 * Other plugins and mods can use this API to register custom traits,
 * query trait information, and interact with the chaos chicken system.
 */
public final class ChaosChickensAPI {

    private static final Map<TraitType, ChaosTrait> traitRegistry = new LinkedHashMap<>();
    private static final Map<TraitType, Double> traitWeights = new HashMap<>();
    private static final List<TraitType> enabledTraits = new ArrayList<>();
    private static volatile boolean bstatsEnabled = true;
    private static volatile boolean initialized = false;

    private ChaosChickensAPI() {
        // Utility class
    }

    /**
     * Initialize the API with default traits.
     * Called internally by the platform-specific module.
     */
    public static synchronized void initialize() {
        if (initialized) return;
        registerDefaultTraits();
        initialized = true;
    }

    /**
     * Register a custom chaos trait.
     *
     * @param trait The trait to register
     * @param weight The spawn weight (higher = more common). Default traits use 1.0.
     * @throws IllegalArgumentException if trait or traitType is null
     */
    public static synchronized void registerTrait(ChaosTrait trait, double weight) {
        if (trait == null) throw new IllegalArgumentException("Trait cannot be null");
        TraitType type = trait.getType();
        if (type == null) throw new IllegalArgumentException("TraitType cannot be null");

        traitRegistry.put(type, trait);
        traitWeights.put(type, weight);
        if (!enabledTraits.contains(type)) {
            enabledTraits.add(type);
        }
    }

    /**
     * Unregister a trait by its type.
     *
     * @param type The trait type to remove
     * @return true if the trait was removed
     */
    public static synchronized boolean unregisterTrait(TraitType type) {
        enabledTraits.remove(type);
        traitWeights.remove(type);
        return traitRegistry.remove(type) != null;
    }

    /**
     * Get a registered trait by type.
     *
     * @param type The trait type
     * @return Optional containing the trait, or empty if not registered
     */
    public static synchronized Optional<ChaosTrait> getTrait(TraitType type) {
        return Optional.ofNullable(traitRegistry.get(type));
    }

    /**
     * Get all registered traits.
     *
     * @return Unmodifiable collection of all registered traits
     */
    public static synchronized Collection<ChaosTrait> getAllTraits() {
        return List.copyOf(traitRegistry.values());
    }

    /**
     * Get all enabled trait types.
     *
     * @return Unmodifiable list of enabled trait types
     */
    public static synchronized List<TraitType> getEnabledTraits() {
        return List.copyOf(enabledTraits);
    }

    /**
     * Enable or disable a specific trait type.
     *
     * @param type The trait type
     * @param enabled true to enable, false to disable
     */
    public static synchronized void setTraitEnabled(TraitType type, boolean enabled) {
        if (enabled && !enabledTraits.contains(type)) {
            enabledTraits.add(type);
        } else if (!enabled) {
            enabledTraits.remove(type);
        }
    }

    /**
     * Check if a trait type is enabled.
     *
     * @param type The trait type
     * @return true if enabled
     */
    public static synchronized boolean isTraitEnabled(TraitType type) {
        return enabledTraits.contains(type);
    }

    /**
     * Get the spawn weight for a trait type.
     *
     * @param type The trait type
     * @return The weight, or 0.0 if not registered
     */
    public static synchronized double getTraitWeight(TraitType type) {
        return traitWeights.getOrDefault(type, 0.0);
    }

    /**
     * Set the spawn weight for a trait type.
     *
     * @param type The trait type
     * @param weight The new weight
     */
    public static synchronized void setTraitWeight(TraitType type, double weight) {
        if (traitRegistry.containsKey(type)) {
            traitWeights.put(type, weight);
        }
    }

    /**
     * Pick a random trait type based on weights.
     * Only enabled traits are considered.
     *
     * @return A random enabled TraitType, or EMPTY if none are enabled
     */
    public static synchronized TraitType pickRandomTrait(Random random) {
        if (random == null) throw new IllegalArgumentException("Random cannot be null");
        
        List<TraitType> pool = new ArrayList<>();
        for (TraitType type : enabledTraits) {
            if (type != TraitType.BOSS && type != TraitType.EMPTY) {
                pool.add(type);
            }
        }
        if (pool.isEmpty()) return TraitType.EMPTY;

        double totalWeight = 0;
        for (TraitType type : pool) {
            totalWeight += traitWeights.getOrDefault(type, 1.0);
        }
        if (totalWeight <= 0.0) return TraitType.EMPTY;

        double roll = random.nextDouble() * totalWeight;
        double current = 0;
        for (TraitType type : pool) {
            current += traitWeights.getOrDefault(type, 1.0);
            if (roll < current) {
                return type;
            }
        }

        return pool.get(pool.size() - 1);
    }

    /**
     * Check if bStats anonymous metrics are enabled.
     *
     * @return true if bStats is enabled
     */
    public static synchronized boolean isBstatsEnabled() {
        return bstatsEnabled;
    }

    /**
     * Set whether bStats anonymous metrics are enabled.
     *
     * @param enabled true to enable bStats, false to disable
     */
    public static synchronized void setBstatsEnabled(boolean enabled) {
        bstatsEnabled = enabled;
    }

    /**
     * Reset the API state. Used for testing or reloading config.
     */
    public static synchronized void reset() {
        traitRegistry.clear();
        traitWeights.clear();
        enabledTraits.clear();
        initialized = false;
    }

    private static void registerDefaultTraits() {
        // Default traits are registered by the platform-specific modules
        // via registerTrait() calls. This method is a hook for any
        // API-level default registrations if needed.
    }
}
