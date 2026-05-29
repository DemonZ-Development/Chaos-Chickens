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
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

import java.util.Collection;

/**
 * Magnet chicken trait.
 * Periodically pulls nearby item entities toward the chicken,
 * with a chance to steal (remove) them.
 */
public class MagnetTrait extends BukkitTrait {

    private static final double MAGNET_RANGE = 6.0;
    private static final double STEAL_CHANCE = 0.30;
    private static final double PULL_SPEED = 0.4;

    public MagnetTrait() {
        super(TraitType.MAGNET, "Attracts nearby items... and sometimes steals them!", 0.8,
                false, true, 10);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        chicken.setCustomName(ChatColor.DARK_AQUA + "Magnet Chicken");
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;

        Collection<Entity> nearbyEntities = chicken.getNearbyEntities(MAGNET_RANGE, MAGNET_RANGE, MAGNET_RANGE);
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Item) {
                Item item = (Item) entity;
                if (item.isDead()) continue;

                // Pull item toward chicken
                org.bukkit.util.Vector diff = chicken.getLocation().toVector()
                        .subtract(item.getLocation().toVector());
                if (diff.lengthSquared() < 0.01) continue;
                org.bukkit.util.Vector direction = diff.normalize().multiply(PULL_SPEED);
                item.setVelocity(direction);

                // Chance to steal (remove) the item
                if (plugin.getRandom().nextDouble() < STEAL_CHANCE) {
                    item.remove();
                }
            }
        }
    }

}
