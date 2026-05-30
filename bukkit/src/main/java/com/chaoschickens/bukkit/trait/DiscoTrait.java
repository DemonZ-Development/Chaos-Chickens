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
import org.bukkit.DyeColor;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Sheep;

/**
 * Disco chicken trait.
 * Plays non-stop cool disco beats and dyes nearby sheep in random colors every tick.
 * Full beat: bass, melody (xylophone/bell/chime/flute), hi-hat.
 */
public class DiscoTrait extends BukkitTrait {

    private static final DyeColor[] COLORS = DyeColor.values();

    public DiscoTrait() {
        super(TraitType.DISCO, "Party time! Plays music and dyes nearby sheep!", 1.0,
                false, true, 10); // Tick every server cycle for non-stop music
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
        int beat = chicken.getTicksLived() % 16;

        // 1. Walking bassline
        float bassPitch = switch (beat % 4) {
            case 0 -> 0.5f;
            case 1 -> 0.7f;
            case 2 -> 0.6f;
            case 3 -> 0.8f;
            default -> 0.5f;
        };
        chicken.getWorld().playSound(chicken.getLocation(),
                Sound.BLOCK_NOTE_BLOCK_BASS, 1.5f, bassPitch);

        // 2. Upbeat melody sequence
        Sound melody = switch (beat % 8) {
            case 0, 4 -> Sound.BLOCK_NOTE_BLOCK_XYLOPHONE;
            case 1, 5 -> Sound.BLOCK_NOTE_BLOCK_BELL;
            case 2, 6 -> Sound.BLOCK_NOTE_BLOCK_CHIME;
            case 3, 7 -> Sound.BLOCK_NOTE_BLOCK_FLUTE;
            default -> Sound.BLOCK_NOTE_BLOCK_XYLOPHONE;
        };
        float melodyPitch = (float) Math.pow(2.0, (6 + (beat % 7)) / 12.0);
        chicken.getWorld().playSound(chicken.getLocation(),
                melody, 1.0f, melodyPitch);

        // 3. Hi-hat percussion every other beat
        if (beat % 2 == 0) {
            chicken.getWorld().playSound(chicken.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.5f);
        }

        // 4. Note particles
        if (plugin.getConfigManager().isTraitParticlesEnabled()) {
            chicken.getWorld().spawnParticle(Particle.NOTE,
                    chicken.getLocation().clone().add(0, 0.8, 0),
                    6, 0.4, 0.4, 0.4, 1.0);
        }

        // 5. Disco floor effect every 4 beats
        if (beat % 4 == 0 && plugin.getConfigManager().isTraitParticlesEnabled()) {
            chicken.getWorld().spawnParticle(Particle.VILLAGER_HAPPY,
                    chicken.getLocation().clone().add(0, 0.3, 0),
                    4, 1.5, 0.1, 1.5, 0.1);
        }

        // 6. Dye ALL nearby sheep EVERY tick
        for (Entity entity : chicken.getNearbyEntities(8, 8, 8)) {
            if (entity instanceof Sheep sheep) {
                DyeColor randomColor = COLORS[rand.nextInt(COLORS.length)];
                sheep.setColor(randomColor);
                sheep.playEffect(EntityEffect.SHEEP_EAT_GRASS);

                // Sparkle on each dyed sheep
                if (plugin.getConfigManager().isTraitParticlesEnabled()) {
                    chicken.getWorld().spawnParticle(Particle.NOTE,
                            sheep.getLocation().clone().add(0, 0.5, 0),
                            2, 0.2, 0.3, 0.2, 0.5);
                }
            }
        }
    }

    @Override
    public void onDeath(Chicken chicken, org.bukkit.event.entity.EntityDeathEvent event) {
        if (chicken == null || event == null) return;
        Material[] discs = {
            Material.MUSIC_DISC_13, Material.MUSIC_DISC_CAT, Material.MUSIC_DISC_BLOCKS,
            Material.MUSIC_DISC_CHIRP, Material.MUSIC_DISC_FAR, Material.MUSIC_DISC_MALL,
            Material.MUSIC_DISC_MELLOHI, Material.MUSIC_DISC_STAL, Material.MUSIC_DISC_STRAD,
            Material.MUSIC_DISC_WARD, Material.MUSIC_DISC_11, Material.MUSIC_DISC_WAIT,
            Material.MUSIC_DISC_OTHERSIDE, Material.MUSIC_DISC_PIGSTEP, Material.MUSIC_DISC_5
        };
        Material disc = discs[plugin.getRandom().nextInt(discs.length)];
        event.getDrops().add(new org.bukkit.inventory.ItemStack(disc, 1));
    }
}
