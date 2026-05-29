/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.forge.trait;

import com.chaoschickens.common.trait.AbstractTrait;
import com.chaoschickens.common.trait.TraitType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

/**
 * Forge-specific abstract trait that extends the common AbstractTrait
 * with Forge/Minecraft-specific lifecycle methods for chicken entities.
 * All Forge trait implementations should extend this class.
 */
public abstract class ForgeTrait extends AbstractTrait {

    protected ForgeTrait(TraitType type, String description, double defaultWeight,
                         boolean hostile, boolean periodic, int tickInterval) {
        super(type, description, defaultWeight, hostile, periodic, tickInterval);
    }

    /**
     * Called when this trait is applied to a chicken entity.
     */
    public abstract void onApply(Chicken chicken);

    /**
     * Called periodically for traits that have isPeriodic() = true.
     */
    public void onTick(Chicken chicken) {
        // Default no-op
    }

    /**
     * Called when a chicken with this trait dies.
     */
    public void onDeath(Chicken chicken, DamageSource source) {
        // Default no-op
    }

    /**
     * Called when a player is near a chicken with this trait.
     */
    public void onPlayerNear(Chicken chicken, Player player) {
        // Default no-op
    }

    /**
     * Called when a chicken's drops are being collected.
     * Used by traits that modify drops (e.g., Golden).
     */
    public void onModifyDrops(Chicken chicken, LivingDropsEvent event) {
        // Default no-op
    }

    /**
     * Get the proximity detection range for this trait.
     * Return 0 to disable proximity detection.
     */
    public double getProximityRange() {
        return 0;
    }
}
