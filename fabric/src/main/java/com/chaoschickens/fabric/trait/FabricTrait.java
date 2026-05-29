/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.fabric.trait;

import com.chaoschickens.common.trait.AbstractTrait;
import com.chaoschickens.common.trait.TraitType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Fabric-specific abstract trait that extends the common AbstractTrait
 * with Fabric/Minecraft-specific lifecycle methods for chicken entities.
 * All Fabric trait implementations should extend this class.
 */
public abstract class FabricTrait extends AbstractTrait {

    /**
     * Construct a new FabricTrait.
     *
     * @param type          The trait type
     * @param description   A short description
     * @param defaultWeight Default spawn weight
     * @param hostile       Whether this trait is hostile
     * @param periodic      Whether this trait has periodic behavior
     * @param tickInterval  Tick interval for periodic behavior
     */
    protected FabricTrait(TraitType type, String description, double defaultWeight,
                          boolean hostile, boolean periodic, int tickInterval) {
        super(type, description, defaultWeight, hostile, periodic, tickInterval);
    }

    /**
     * Called when this trait is applied to a chicken entity.
     * Used to set initial effects like custom names, attributes, etc.
     *
     * @param chicken The chicken entity the trait is applied to
     */
    public abstract void onApply(ChickenEntity chicken);

    /**
     * Called periodically for traits that have isPeriodic() = true.
     * The interval is determined by getTickInterval().
     *
     * @param chicken The chicken entity to tick
     */
    public void onTick(ChickenEntity chicken) {
        // Default no-op; override in periodic traits
    }

    /**
     * Called when a chicken with this trait dies.
     *
     * @param chicken The chicken entity that died
     * @param source  The damage source that caused the death
     */
    public void onDeath(ChickenEntity chicken, DamageSource source) {
        // Default no-op; override in traits with death behavior
    }

    /**
     * Called when a player is near a chicken with this trait.
     * The proximity check is handled by the main tick loop.
     *
     * @param chicken The chicken entity
     * @param player  The nearby player
     */
    public void onPlayerNear(ChickenEntity chicken, PlayerEntity player) {
        // Default no-op; override in proximity-based traits
    }

    /**
     * Get the proximity detection range for this trait.
     * Return 0 to disable proximity detection.
     *
     * @return The range in blocks, or 0 to disable
     */
    public double getProximityRange() {
        return 0;
    }
}
