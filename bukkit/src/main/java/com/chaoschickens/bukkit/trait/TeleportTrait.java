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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Chicken;

/**
 * Teleport chicken trait.
 * Teleports like an enderman: 3-8 block range, safe surface detection,
 * no suffocation, no lava landing.
 */
public class TeleportTrait extends BukkitTrait {

    private static final double MIN_RANGE = 3.0;
    private static final double MAX_RANGE = 8.0;
    private static final int MAX_ATTEMPTS = 20;

    public TeleportTrait() {
        super(TraitType.TELEPORT, "Teleports around randomly! Hard to catch!", 0.6,
                false, true, 160); // Every 8 seconds
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        chicken.setCustomName(ChatColor.DARK_PURPLE + "Teleport Chicken");
        chicken.setCustomNameVisible(true);
        if (chicken.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) != null) {
            chicken.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(16.0);
            chicken.setHealth(16.0);
        }
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        teleport(chicken);
    }

    /**
     * Perform a safe teleport. Public so BossTrait can call it.
     */
    public void teleport(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;

        java.util.Random random = plugin.getRandom();
        Location chickenLoc = chicken.getLocation();

        // Pre-teleport particles
        if (plugin.getConfigManager().isTraitParticlesEnabled()) {
            chicken.getWorld().spawnParticle(Particle.PORTAL,
                    chickenLoc.clone().add(0, 0.5, 0),
                    20, 0.3, 0.5, 0.3, 0.5);
            chicken.getWorld().spawnParticle(Particle.PORTAL,
                    chickenLoc.clone().add(0, 0.5, 0),
                    10, 0.2, 0.3, 0.2, 0.3);
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            // Polar coordinates for enderman-like spread
            double angle = random.nextDouble() * 2.0 * Math.PI;
            double distance = MIN_RANGE + random.nextDouble() * (MAX_RANGE - MIN_RANGE);

            double newX = chickenLoc.getX() + Math.cos(angle) * distance;
            double newZ = chickenLoc.getZ() + Math.sin(angle) * distance;
            double baseY = chickenLoc.getY();

            // Scan for safe surface (±10 Y)
            for (int dy = 0; dy <= 10; dy++) {
                for (int sign : new int[]{1, -1}) {
                    double checkY = baseY + (dy * sign);
                    if (checkY < chickenLoc.getWorld().getMinHeight()
                            || checkY > chickenLoc.getWorld().getMaxHeight() - 2) continue;

                    Location loc = new Location(chickenLoc.getWorld(), newX, checkY, newZ);

                    // Check chunk loaded
                    if (!loc.getChunk().isLoaded()) continue;

                    Block ground = loc.clone().add(0, -1, 0).getBlock();
                    Block feet = loc.getBlock();
                    Block head = loc.clone().add(0, 1, 0).getBlock();

                    if (ground.getType().isSolid()
                            && feet.getType().isAir()
                            && head.getType().isAir()
                            && !isDangerous(ground.getType())) {

                        chicken.teleport(loc);

                        // Post-teleport effects
                        chicken.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.2f);
                        if (plugin.getConfigManager().isTraitParticlesEnabled()) {
                            chicken.getWorld().spawnParticle(Particle.PORTAL,
                                    loc.clone().add(0, 0.5, 0),
                                    20, 0.3, 0.5, 0.3, 0.5);
                            chicken.getWorld().spawnParticle(Particle.PORTAL,
                                    loc.clone().add(0, 0.5, 0),
                                    10, 0.2, 0.3, 0.2, 0.3);
                        }
                        return; // Success
                    }
                }
            }
        }
        // All attempts failed — chicken stays put
    }

    private boolean isDangerous(Material type) {
        return type == Material.LAVA
                || type == Material.FIRE
                || type == Material.SOUL_FIRE
                || type == Material.CACTUS
                || type == Material.MAGMA_BLOCK
                || type == Material.SWEET_BERRY_BUSH
                || type == Material.POWDER_SNOW;
    }
}
