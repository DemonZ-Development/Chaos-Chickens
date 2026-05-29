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

import com.chaoschickens.bukkit.ChaosChickensBukkit;
import com.chaoschickens.common.trait.TraitType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Chicken;

/**
 * Teleport chicken trait.
 * Periodically teleports to a random nearby location with ender particles.
 * Bug #9 fix: Uses shared Random from plugin.
 * Bug #10 fix: Added comprehensive safety checks for teleport destination.
 */
public class TeleportTrait extends BukkitTrait {

    private static final double TELEPORT_RANGE = 15.0;

    public TeleportTrait() {
        super(TraitType.TELEPORT, "Teleports around randomly! Hard to catch!", 0.6,
                false, true, 200);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        chicken.setCustomName(ChatColor.DARK_PURPLE + "Teleport Chicken");
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;

        java.util.Random random = plugin.getRandom();

        // Spawn ender particles at current location before teleporting (use clone to avoid mutation) (if enabled)
        if (plugin.getConfigManager().isTraitParticlesEnabled()) {
            Location particleLoc = chicken.getLocation().clone().add(0, 1, 0);
            chicken.getWorld().spawnParticle(
                    Particle.PORTAL,
                    particleLoc,
                    30,
                    0.5, 0.5, 0.5,
                    0.5
            );
        }

        // Calculate random offset within range
        double offsetX = (random.nextDouble() - 0.5) * 2 * TELEPORT_RANGE;
        double offsetY = random.nextDouble() * 5; // Allow some vertical teleport
        double offsetZ = (random.nextDouble() - 0.5) * 2 * TELEPORT_RANGE;

        Location chickenLoc = chicken.getLocation();
        Location newLoc = new Location(
                chickenLoc.getWorld(),
                chickenLoc.getX() + offsetX,
                chickenLoc.getY() + offsetY,
                chickenLoc.getZ() + offsetZ
        );

        // Bug #10 fix: Comprehensive safety checks for teleport destination
        if (isSafeTeleportDestination(newLoc)) {
            chicken.teleport(newLoc);

            // Spawn ender particles at new location (if enabled)
            if (plugin.getConfigManager().isTraitParticlesEnabled()) {
                Location newParticleLoc = chicken.getLocation().clone().add(0, 1, 0);
                chicken.getWorld().spawnParticle(
                        Particle.PORTAL,
                        newParticleLoc,
                        30,
                        0.5, 0.5, 0.5,
                        0.5
                );
            }
        }
    }

    /**
     * Check if a location is safe for teleportation.
     * Bug #10 fix: Validates world bounds, air blocks, no dangerous blocks, and loaded chunks.
     *
     * @param loc The location to check
     * @return true if the location is safe for teleportation
     */
    private boolean isSafeTeleportDestination(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;

        // Check world bounds
        if (loc.getY() < loc.getWorld().getMinHeight() ||
                loc.getY() > loc.getWorld().getMaxHeight() - 2) {
            return false;
        }

        // Check if chunk is loaded
        if (!loc.getChunk().isLoaded()) {
            return false;
        }

        Block feet = loc.getBlock();
        Block head = loc.clone().add(0, 1, 0).getBlock();
        Block below = loc.clone().add(0, -1, 0).getBlock();

        // Feet and head must be air (or passable)
        if (!feet.getType().isAir() || !head.getType().isAir()) {
            return false;
        }

        // Must have solid ground below
        if (!below.getType().isSolid()) {
            return false;
        }

        // Check for dangerous blocks nearby
        Material belowType = below.getType();
        if (belowType == Material.LAVA ||
                belowType == Material.CACTUS ||
                belowType == Material.FIRE ||
                belowType == Material.SOUL_FIRE ||
                belowType == Material.MAGMA_BLOCK ||
                belowType == Material.SWEET_BERRY_BUSH ||
                belowType == Material.POWDER_SNOW) {
            return false;
        }

        return true;
    }

}
