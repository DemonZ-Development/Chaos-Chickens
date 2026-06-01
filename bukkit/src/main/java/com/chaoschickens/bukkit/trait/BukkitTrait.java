/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.bukkit.trait;

import com.chaoschickens.common.trait.AbstractTrait;
import com.chaoschickens.common.trait.TraitType;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Bukkit-specific abstract trait that extends the common AbstractTrait
 * with Bukkit-specific lifecycle methods for chicken entities.
 */
public abstract class BukkitTrait extends AbstractTrait {

    protected static com.chaoschickens.bukkit.ChaosChickensBukkit plugin;

    public static void setPlugin(com.chaoschickens.bukkit.ChaosChickensBukkit p) {
        plugin = p;
    }

    /**
     * Construct a new BukkitTrait.
     *
     * @param type          The trait type
     * @param description   A short description
     * @param defaultWeight Default spawn weight
     * @param hostile       Whether this trait is hostile
     * @param periodic      Whether this trait has periodic behavior
     * @param tickInterval  Tick interval for periodic behavior
     */
    protected BukkitTrait(TraitType type, String description, double defaultWeight,
                          boolean hostile, boolean periodic, int tickInterval) {
        super(type, description, defaultWeight, hostile, periodic, tickInterval);
    }

    /**
     * Called when this trait is applied to a chicken entity.
     * Used to set initial effects like custom names, attributes, etc.
     *
     * @param chicken The chicken entity the trait is applied to
     */
    public abstract void onApply(Chicken chicken);

    /**
     * Called periodically for traits that have isPeriodic() = true.
     * The interval is determined by getTickInterval().
     *
     * @param chicken The chicken entity to tick
     */
    public void onTick(Chicken chicken) {
        // Default no-op; override in periodic traits
    }

    /**
     * Called when a chicken with this trait dies.
     *
     * @param chicken The chicken entity that died
     * @param event   The EntityDeathEvent
     */
    public void onDeath(Chicken chicken, EntityDeathEvent event) {
        // Default no-op; override in traits with death behavior
    }

    /**
     * Called when a player is near a chicken with this trait.
     * The proximity check is handled by the main tick loop.
     *
     * @param chicken The chicken entity
     * @param player  The nearby player
     */
    public void onPlayerNear(Chicken chicken, Player player) {
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

    /**
     * Called when a chicken with this trait takes damage.
     *
     * @param chicken The chicken entity
     * @param event   The EntityDamageEvent
     * @param amount  The damage amount
     */
    public void onDamage(Chicken chicken, org.bukkit.event.entity.EntityDamageEvent event, double amount) {
        // Default no-op; override in traits with damage behavior
    }
}
