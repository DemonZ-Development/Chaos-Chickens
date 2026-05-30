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
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.ChatFormatting;

import java.util.List;

/**
 * Disco chicken trait.
 * Plays random note block sounds and dyes nearby sheep in random colors.
 */
public class DiscoTrait extends FabricTrait {

    /** Range to find sheep for dyeing. */
    private static final double SHEEP_DYE_RANGE = 6.0;

    public DiscoTrait() {
        super(TraitType.DISCO, "Party time! Plays music and dyes sheep!", 1.0,
                false, true, 5); // 5 ticks = 0.25 seconds (Fast 120 BPM disco beat!)
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
        if (!(chicken.level() instanceof ServerLevel serverWorld)) return;

        var random = chicken.getRandom();

        // 1. Play walking bassline attached to the chicken
        net.minecraft.sounds.SoundEvent bass = SoundEvents.NOTE_BLOCK_BASS.value();
        float bassPitch = (float) Math.pow(2.0, ((chicken.tickCount % 4) * 2 - 4) / 12.0); // Walking bass pitch loop
        serverWorld.playSound(
                null,
                chicken,
                bass,
                net.minecraft.sounds.SoundSource.RECORDS,
                1.2f, bassPitch
        );

        // 2. Play upbeat melody sequence attached to the chicken
        net.minecraft.sounds.SoundEvent melody = switch ((chicken.tickCount / 2) % 4) {
            case 0 -> SoundEvents.NOTE_BLOCK_XYLOPHONE.value();
            case 1 -> SoundEvents.NOTE_BLOCK_BELL.value();
            case 2 -> SoundEvents.NOTE_BLOCK_CHIME.value();
            case 3 -> SoundEvents.NOTE_BLOCK_FLUTE.value();
            default -> SoundEvents.NOTE_BLOCK_XYLOPHONE.value();
        };
        float melodyPitch = (float) Math.pow(2.0, (random.nextInt(8) + 2) / 12.0); // High-register scale
        serverWorld.playSound(
                null,
                chicken,
                melody,
                net.minecraft.sounds.SoundSource.RECORDS,
                0.9f, melodyPitch
        );

        // 3. Spawn colorful note particles
        serverWorld.sendParticles(
                ParticleTypes.NOTE,
                chicken.getX(), chicken.getY() + 0.5, chicken.getZ(),
                8,
                0.5, 0.5, 0.5,
                1.0
        );

        // 4. Dye nearby sheep every 4 ticks (1 second) to match the beat
        if (chicken.tickCount % 4 == 0) {
            List<Sheep> nearbySheep = serverWorld.getEntitiesOfClass(
                    Sheep.class,
                    chicken.getBoundingBox().inflate(SHEEP_DYE_RANGE),
                    sheep -> sheep.isAlive()
            );

            for (Sheep sheep : nearbySheep) {
                DyeColor newColor = DyeColor.values()[chicken.getRandom().nextInt(DyeColor.values().length)];
                sheep.setColor(newColor);
            }
        }
    }
}
