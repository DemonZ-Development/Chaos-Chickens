/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.forge.trait;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;

import java.util.List;

/**
 * Disco chicken trait.
 * Plays non-stop cool disco beats and dyes nearby sheep in random colors every tick.
 */
public class DiscoTrait extends ForgeTrait {

    private static final double SHEEP_DYE_RANGE = 8.0;
    private static final DyeColor[] DYE_COLORS = DyeColor.values();

    public DiscoTrait() {
        super(TraitType.DISCO, "Party time! Plays music and dyes sheep!", 1.0,
                false, true, 10); // Tick every server cycle
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Component.literal("Disco Chicken").withStyle(ChatFormatting.LIGHT_PURPLE));
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.level() instanceof ServerLevel serverLevel)) return;

        var random = chicken.getRandom();
        int beat = (int)(serverLevel.getGameTime() % 16);

        // 1. Walking bassline
        float bassPitch = switch (beat % 4) {
            case 0 -> 0.5f;
            case 1 -> 0.7f;
            case 2 -> 0.6f;
            case 3 -> 0.8f;
            default -> 0.5f;
        };
        serverLevel.playSound(null, chicken.getX(), chicken.getY(), chicken.getZ(),
                SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.RECORDS, 1.5f, bassPitch);

        // 2. Upbeat melody sequence
        net.minecraft.sounds.SoundEvent melody = switch (beat % 8) {
            case 0, 4 -> SoundEvents.NOTE_BLOCK_XYLOPHONE.get();
            case 1, 5 -> SoundEvents.NOTE_BLOCK_BELL.get();
            case 2, 6 -> SoundEvents.NOTE_BLOCK_CHIME.get();
            case 3, 7 -> SoundEvents.NOTE_BLOCK_FLUTE.get();
            default -> SoundEvents.NOTE_BLOCK_XYLOPHONE.get();
        };
        float melodyPitch = (float) Math.pow(2.0, (6 + (beat % 7)) / 12.0);
        serverLevel.playSound(null, chicken.getX(), chicken.getY(), chicken.getZ(),
                melody, SoundSource.RECORDS, 1.0f, melodyPitch);

        // 3. Hi-hat percussion every other beat
        if (beat % 2 == 0) {
            serverLevel.playSound(null, chicken.getX(), chicken.getY(), chicken.getZ(),
                    SoundEvents.NOTE_BLOCK_HAT.get(), SoundSource.RECORDS, 0.8f, 1.5f);
        }

        // 4. Note particles
        serverLevel.sendParticles(ParticleTypes.NOTE,
                chicken.getX(), chicken.getY() + 0.8, chicken.getZ(),
                6, 0.4, 0.4, 0.4, 1.0);

        // 5. Disco floor effect
        if (beat % 4 == 0) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    chicken.getX(), chicken.getY() + 0.3, chicken.getZ(),
                    4, 1.5, 0.1, 1.5, 0.1);
        }

        // 6. Dye ALL nearby sheep EVERY tick
        List<Sheep> nearbySheep = serverLevel.getEntitiesOfClass(
                Sheep.class,
                chicken.getBoundingBox().inflate(SHEEP_DYE_RANGE),
                sheep -> sheep.isAlive() && !sheep.isSheared());

        for (Sheep sheep : nearbySheep) {
            DyeColor newColor = DYE_COLORS[random.nextInt(DYE_COLORS.length)];
            sheep.setColor(newColor);

            // Sparkle on each dyed sheep
            serverLevel.sendParticles(ParticleTypes.NOTE,
                    sheep.getX(), sheep.getY() + 0.5, sheep.getZ(),
                    2, 0.2, 0.3, 0.2, 0.5);
        }
    }

    @Override
    public void onDeath(Chicken chicken, net.minecraft.world.damagesource.DamageSource source) {
        if (chicken == null) return;
        if (!(chicken.level() instanceof ServerLevel serverLevel)) return;

        net.minecraft.world.item.Item[] discs = {
            net.minecraft.world.item.Items.MUSIC_DISC_13, net.minecraft.world.item.Items.MUSIC_DISC_CAT,
            net.minecraft.world.item.Items.MUSIC_DISC_BLOCKS, net.minecraft.world.item.Items.MUSIC_DISC_CHIRP,
            net.minecraft.world.item.Items.MUSIC_DISC_FAR, net.minecraft.world.item.Items.MUSIC_DISC_MALL,
            net.minecraft.world.item.Items.MUSIC_DISC_MELLOHI, net.minecraft.world.item.Items.MUSIC_DISC_STAL,
            net.minecraft.world.item.Items.MUSIC_DISC_STRAD, net.minecraft.world.item.Items.MUSIC_DISC_WARD,
            net.minecraft.world.item.Items.MUSIC_DISC_11, net.minecraft.world.item.Items.MUSIC_DISC_WAIT,
            net.minecraft.world.item.Items.MUSIC_DISC_OTHERSIDE, net.minecraft.world.item.Items.MUSIC_DISC_PIGSTEP,
            net.minecraft.world.item.Items.MUSIC_DISC_5
        };
        net.minecraft.world.item.Item disc = discs[chicken.getRandom().nextInt(discs.length)];
        chicken.spawnAtLocation(new net.minecraft.world.item.ItemStack(disc, 1));
    }
}
