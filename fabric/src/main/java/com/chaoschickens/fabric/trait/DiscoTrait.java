/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.fabric.trait;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Disco chicken trait.
 * Plays non-stop cool disco beats attached to the chicken, dyes nearby sheep in random colors,
 * and drops a random music disc on death.
 */
public class DiscoTrait extends FabricTrait {

    /** Range to find sheep for dyeing. */
    private static final double SHEEP_DYE_RANGE = 8.0;

    /** DyeColor values cached for performance. */
    private static final DyeColor[] DYE_COLORS = DyeColor.values();

    public DiscoTrait() {
        super(TraitType.DISCO, "Party time! Plays music and dyes sheep!", 1.0,
                false, true, 10); // Ticks every server cycle
    }

    @Override
    public void onApply(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Text.literal("Disco Chicken").formatted(Formatting.LIGHT_PURPLE));
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.getWorld() instanceof ServerWorld serverWorld)) return;

        var random = chicken.getRandom();
        int beat = (int)(serverWorld.getTime() % 16); // Use world time for consistent beat

        // 1. Play walking bassline attached to the chicken (moves with it)
        float bassPitch = switch (beat % 4) {
            case 0 -> 0.5f;
            case 1 -> 0.7f;
            case 2 -> 0.6f;
            case 3 -> 0.8f;
            default -> 0.5f;
        };
        serverWorld.playSound(
                null,
                chicken.getX(), chicken.getY(), chicken.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                SoundCategory.RECORDS,
                1.5f, bassPitch
        );

        // 2. Play upbeat melody sequence attached to the chicken
        net.minecraft.sound.SoundEvent melody = switch (beat % 8) {
            case 0, 4 -> SoundEvents.BLOCK_NOTE_BLOCK_XYLOPHONE.value();
            case 1, 5 -> SoundEvents.BLOCK_NOTE_BLOCK_BELL.value();
            case 2, 6 -> SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value();
            case 3, 7 -> SoundEvents.BLOCK_NOTE_BLOCK_FLUTE.value();
            default -> SoundEvents.BLOCK_NOTE_BLOCK_XYLOPHONE.value();
        };
        float melodyPitch = (float) Math.pow(2.0, (6 + (beat % 7)) / 12.0);
        serverWorld.playSound(
                null,
                chicken.getX(), chicken.getY(), chicken.getZ(),
                melody,
                SoundCategory.RECORDS,
                1.0f, melodyPitch
        );

        // 3. Hi-hat percussion on every other beat
        if (beat % 2 == 0) {
            serverWorld.playSound(
                    null,
                    chicken.getX(), chicken.getY(), chicken.getZ(),
                    SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(),
                    SoundCategory.RECORDS,
                    0.8f, 1.5f
            );
        }

        // 4. Spawn colorful note particles
        if (com.chaoschickens.fabric.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            serverWorld.spawnParticles(
                    ParticleTypes.NOTE,
                    chicken.getX(), chicken.getY() + 0.8, chicken.getZ(),
                    6,
                    0.4, 0.4, 0.4,
                    1.0
            );
        }

        // 5. Disco floor effect particles
        if (beat % 4 == 0 && com.chaoschickens.fabric.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            serverWorld.spawnParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    chicken.getX(), chicken.getY() + 0.3, chicken.getZ(),
                    4,
                    1.5, 0.1, 1.5,
                    0.1
            );
        }

        // 6. Dye ALL nearby sheep EVERY tick
        List<SheepEntity> nearbySheep = serverWorld.getEntitiesByClass(
                SheepEntity.class,
                chicken.getBoundingBox().expand(SHEEP_DYE_RANGE),
                sheep -> sheep.isAlive() && !sheep.isSheared()
        );

        for (SheepEntity sheep : nearbySheep) {
            DyeColor newColor = DYE_COLORS[random.nextInt(DYE_COLORS.length)];
            sheep.setColor(newColor);

            if (com.chaoschickens.fabric.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
                serverWorld.spawnParticles(
                        ParticleTypes.NOTE,
                        sheep.getX(), sheep.getY() + 0.5, sheep.getZ(),
                        2,
                        0.2, 0.3, 0.2,
                        0.5
                );
            }
        }
    }

    @Override
    public void onDeath(ChickenEntity chicken, DamageSource source) {
        if (chicken == null) return;
        if (!(chicken.getWorld() instanceof ServerWorld)) return;

        net.minecraft.item.Item[] discs = {
            Items.MUSIC_DISC_13, Items.MUSIC_DISC_CAT,
            Items.MUSIC_DISC_BLOCKS, Items.MUSIC_DISC_CHIRP,
            Items.MUSIC_DISC_FAR, Items.MUSIC_DISC_MALL,
            Items.MUSIC_DISC_MELLOHI, Items.MUSIC_DISC_STAL,
            Items.MUSIC_DISC_STRAD, Items.MUSIC_DISC_WARD,
            Items.MUSIC_DISC_11, Items.MUSIC_DISC_WAIT,
            Items.MUSIC_DISC_OTHERSIDE, Items.MUSIC_DISC_PIGSTEP,
            Items.MUSIC_DISC_5
        };
        net.minecraft.item.Item disc = discs[chicken.getRandom().nextInt(discs.length)];
        chicken.dropStack(new ItemStack(disc, 1));
    }
}
