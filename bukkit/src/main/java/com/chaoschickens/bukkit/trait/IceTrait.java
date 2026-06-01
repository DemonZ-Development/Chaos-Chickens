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
import org.bukkit.block.Block;
import org.bukkit.entity.Chicken;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Ice chicken trait.
 * Freezes water blocks it walks on and drops ice on death.
 * Displays snowflake particles around it.
 * Fix: Uses Location.clone() to avoid mutating the entity's actual location.
 */
public class IceTrait extends BukkitTrait {

    /** Range to check for water blocks to freeze. */
    private static final int FREEZE_CHECK_RANGE = 1;

    public IceTrait() {
        super(TraitType.ICE, "A freezing chicken that turns water to ice!", 0.8,
                false, true, 20);
    }

    @Override
    public boolean modifiesDrops() {
        return true;
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        chicken.setCustomName(ChatColor.AQUA + "Ice Chicken");
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;

        Location chickenLoc = chicken.getLocation(); // Do NOT mutate this

        // Freeze water blocks around the chicken (but not the block directly below)
        // Uses FROSTED_ICE which auto-melts, matching Fabric-26 behavior
        for (int dx = -FREEZE_CHECK_RANGE; dx <= FREEZE_CHECK_RANGE; dx++) {
            for (int dz = -FREEZE_CHECK_RANGE; dz <= FREEZE_CHECK_RANGE; dz++) {
                Block checkBlock = chickenLoc.clone().add(dx, -1, dz).getBlock();
                // Skip the block directly under the chicken to prevent self-trapping
                if (dx == 0 && dz == 0) continue;
                if (checkBlock.getType() == Material.WATER) {
                    checkBlock.setType(Material.FROSTED_ICE);
                }
            }
        }

        // Display snowflake particles around the chicken (use clone) (if enabled)
        if (plugin.getConfigManager().isTraitParticlesEnabled()) {
            chicken.getWorld().spawnParticle(
                    Particle.SNOWFLAKE,
                    chickenLoc.clone().add(0, 0.5, 0),
                    4,
                    0.3, 0.3, 0.3,
                    0.02
            );
        }
    }

    @Override
    public void onDeath(Chicken chicken, EntityDeathEvent event) {
        if (chicken == null || event == null) return;

        // Freeze nearest blocks into ice (2-block radius)
        Location loc = chicken.getLocation();
        int radius = 2;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = loc.clone().add(x, y, z).getBlock();
                    Material type = block.getType();
                    if (!type.isAir() && type != Material.ICE && type != Material.FROSTED_ICE
                            && type != Material.BEDROCK && type != Material.OBSIDIAN && type != Material.BARRIER) {
                        plugin.registerBlockRestore(block, type, block.getBlockData(), 200);
                        block.setType(Material.ICE);
                    }
                }
            }
        }

        // Drop ice items matching Fabric-26 logic
        int count = 1 + plugin.getRandom().nextInt(3); // 1-3 ice
        event.getDrops().add(new ItemStack(Material.ICE, count));
    }
}
