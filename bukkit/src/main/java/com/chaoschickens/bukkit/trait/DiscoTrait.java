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
import org.bukkit.DyeColor;
import org.bukkit.EntityEffect;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Sheep;

/**
 * Disco chicken trait.
 * Plays random note block sounds periodically and changes nearby sheep colors.
 * Bug #8 fix: Removed shared localTickCounter that caused cross-chicken state leakage.
 * Now uses chicken.getTicksLived() for sheep dye timing.
 * Bug #9 fix: Uses shared Random from plugin instance.
 */
public class DiscoTrait extends BukkitTrait {

    public DiscoTrait() {
        super(TraitType.DISCO, "Party time! Plays music and dyes nearby sheep!", 1.0,
                false, true, 15);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        chicken.setCustomName(ChatColor.LIGHT_PURPLE + "Disco Chicken");
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;

        java.util.Random rand = plugin.getRandom();

        // Play random note block sound
        float pitch = 0.5f + rand.nextFloat() * 1.5f;
        chicken.getWorld().playSound(
                chicken.getLocation(),
                org.bukkit.Sound.BLOCK_NOTE_BLOCK_HARP,
                0.7f,
                pitch
        );

        // Change color of nearby sheep every other onTick call using chicken's ticks lived
        if (chicken.getTicksLived() % 2 == 0) {
            for (Entity entity : chicken.getNearbyEntities(8, 8, 8)) {
                if (entity instanceof Sheep) {
                    Sheep sheep = (Sheep) entity;
                    DyeColor[] colors = DyeColor.values();
                    DyeColor randomColor = colors[rand.nextInt(colors.length)];
                    sheep.setColor(randomColor);
                    sheep.playEffect(EntityEffect.SHEEP_EAT_GRASS);
                }
            }
        }
    }

}
