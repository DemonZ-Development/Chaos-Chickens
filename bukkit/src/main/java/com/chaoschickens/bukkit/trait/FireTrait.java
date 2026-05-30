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
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;

/**
 * Fire chicken trait.
 * Sets nearby players on fire, spawns flame particles, and is IMMUNE to fire/lava.
 * Continuously suppresses fire ticks and heals when in lava.
 */
public class FireTrait extends BukkitTrait {

    private static final double PROXIMITY_RANGE = 5.0;
    private static final int FIRE_TICKS = 60; // 3 seconds

    public FireTrait() {
        super(TraitType.FIRE, "Burns nearby players with its fiery aura!", 1.0,
                true, true, 10); // Tick frequently to keep fire immunity active
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        chicken.setCustomName(ChatColor.GOLD + "Fire Chicken");
        chicken.setCustomNameVisible(true);
        // Immediately clear any fire
        chicken.setFireTicks(0);
        chicken.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, 200, 0, false, false));
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;

        // Refresh fire resistance on every tick call (ticked every 10 ticks, 200 tick duration is a safe refresh)
        chicken.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, 200, 0, false, false));

        // === CRITICAL: Continuously suppress fire ===
        if (chicken.getFireTicks() > 0) {
            chicken.setFireTicks(0);
        }

        // If in lava, heal the chicken (fire chickens THRIVE in lava)
        org.bukkit.block.Block block = chicken.getLocation().getBlock();
        boolean inLava = block.getType() == Material.LAVA;
        if (!inLava) {
            // Also check block below (standing on lava)
            inLava = chicken.getLocation().clone().add(0, -0.5, 0).getBlock().getType() == Material.LAVA;
        }

        if (inLava) {
            chicken.setFireTicks(0);
            double maxHealth = chicken.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            if (chicken.getHealth() < maxHealth) {
                chicken.setHealth(Math.min(maxHealth, chicken.getHealth() + 0.5));
            }
            // Lava swimming particles
            if (plugin.getConfigManager().isTraitParticlesEnabled()) {
                chicken.getWorld().spawnParticle(Particle.LAVA,
                        chicken.getLocation().clone().add(0, 0.3, 0),
                        3, 0.2, 0.1, 0.2, 0.0);
            }
        }

        // Leaves fire while walking!
        if (chicken.isOnGround()) {
            org.bukkit.block.Block currentBlock = chicken.getLocation().getBlock();
            org.bukkit.block.Block belowBlock = currentBlock.getRelative(org.bukkit.block.BlockFace.DOWN);
            if (currentBlock.getType() == Material.AIR && belowBlock.getType().isSolid() && belowBlock.getType() != Material.FIRE) {
                currentBlock.setType(Material.FIRE);
            }
        }

        // Display fire particles
        if (plugin.getConfigManager().isTraitParticlesEnabled()) {
            chicken.getWorld().spawnParticle(
                    Particle.FLAME,
                    chicken.getLocation().clone().add(0, 0.5, 0),
                    5, 0.3, 0.3, 0.3, 0.02);
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
