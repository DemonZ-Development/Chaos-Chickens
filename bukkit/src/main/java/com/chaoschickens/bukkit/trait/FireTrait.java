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

import com.chaoschickens.common.trait.TraitType;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;

/**
 * Fire chicken trait.
 * Sets nearby players on fire and displays fire particles.
 */
public class FireTrait extends BukkitTrait {

    private static final double PROXIMITY_RANGE = 5.0;
    private static final int FIRE_TICKS = 60; // 3 seconds

    public FireTrait() {
        super(TraitType.FIRE, "Burns nearby players with its fiery aura!", 1.0,
                true, true, 20);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        chicken.setCustomName(ChatColor.GOLD + "Fire Chicken");
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        if (plugin.getConfigManager().isTraitParticlesEnabled()) {
            // Display fire particles around the chicken
            chicken.getWorld().spawnParticle(
                    Particle.FLAME,
                    chicken.getLocation().add(0, 0.5, 0),
                    8,      // count
                    0.3,    // offsetX
                    0.3,    // offsetY
                    0.3,    // offsetZ
                    0.02    // speed
            );
        }
    }

    @Override
    public void onPlayerNear(Chicken chicken, Player player) {
        if (chicken == null || chicken.isDead() || player == null) return;
        player.setFireTicks(FIRE_TICKS);
    }

    @Override
    public double getProximityRange() {
        return PROXIMITY_RANGE;
    }
}
