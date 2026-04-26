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
package com.chaoschickens.common.trait;

/**
 * Boss trait implementation — the ultimate chaos chicken.
 *
 * <p>A Boss Chicken combines multiple random traits into a single formidable entity.
 * When spawned, a Boss Chicken selects between 2 and 5 additional traits (configurable
 * via {@link com.chaoschickens.common.config.ConfigManager#getBossTraitCount()}),
 * gaining the combined effects of all of them. Boss Chickens are hostile, periodically
 * active, and extremely rare.</p>
 *
 * <p>Platform-specific modules should extend this class to implement the actual
 * boss behavior (name formatting, particle effects, multi-trait application, etc.).</p>
 */
public class BossTrait extends AbstractTrait {

    /**
     * Constructs a new BossTrait with the following defaults:
     * <ul>
     *   <li>Type: {@link TraitType#BOSS}</li>
     *   <li>Hostile: true — Boss Chickens attack players</li>
     *   <li>Periodic: true — Boss abilities activate on a timer</li>
     *   <li>Tick interval: 100 ticks (5 seconds) — slower than normal periodic traits</li>
     *   <li>Default weight: 0.02 — extremely rare spawn chance</li>
     * </ul>
     */
    public BossTrait() {
        super(TraitType.BOSS,
              "A terrifying Boss Chicken that combines multiple traits into one "
              + "formidable entity. Hostile, periodic, and extremely rare. "
              + "Spawns with 2-5 additional random traits active simultaneously.",
              0.02,
              true,
              true,
              100);
    }

    /**
     * Get the number of additional traits this boss chicken should have.
     * Defaults to the configured boss trait count from ConfigManager.
     *
     * @return The number of additional traits (typically 2-5)
     */
    public int getTraitCount() {
        return 3; // Default; platform modules can override using ConfigManager
    }

    /**
     * Boss chickens can stack with other traits by definition —
     * they are the container for multiple combined traits.
     *
     * @return always true
     */
    @Override
    public boolean canStack() {
        return true;
    }

    /**
     * Boss chickens modify drops — they drop enhanced loot based on
     * their combined traits.
     *
     * @return always true
     */
    @Override
    public boolean modifiesDrops() {
        return true;
    }
}
