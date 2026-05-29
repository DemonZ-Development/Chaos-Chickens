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

/**
 * Explosive chicken trait.
 * Creates a small explosion on death without destroying blocks.
 */
public class ExplosiveTrait extends BukkitTrait {

    public ExplosiveTrait() {
        super(TraitType.EXPLOSIVE, "Explodes on death! Careful around these chickens.", 1.0,
                true, false, 20);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        chicken.setCustomName(ChatColor.RED + "Explosive Chicken");
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onDeath(Chicken chicken, org.bukkit.event.entity.EntityDeathEvent event) {
        if (chicken == null) return;
        // Create explosion at death location: power 2.0, no fire, no block damage
        chicken.getWorld().createExplosion(
                chicken.getLocation(),
                2.0f,
                false,  // no fire
                false   // no block damage
        );
    }
}
