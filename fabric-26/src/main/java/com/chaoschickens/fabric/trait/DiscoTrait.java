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
                false, true, 15);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Component.literal("Disco Chicken").withStyle(ChatFormatting.LIGHT_PURPLE));
        chicken.setCustomNameVisible(true);

        if (chicken.level() instanceof ServerLevel serverWorld) {
            serverWorld.playSound(
                    null,
                    chicken.getX(), chicken.getY(), chicken.getZ(),
                    SoundEvents.MUSIC_DISC_PIGSTEP.value(),
                    net.minecraft.sounds.SoundSource.RECORDS,
                    1.5f, 1.0f
            );
        }
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.level() instanceof ServerLevel serverWorld)) return;

        // Play random note block sound with true musical scale sequence!
        var random = chicken.getRandom();
        net.minecraft.sounds.SoundEvent sound = switch (random.nextInt(6)) {
            case 0 -> SoundEvents.NOTE_BLOCK_HARP.value();
            case 1 -> SoundEvents.NOTE_BLOCK_BASS.value();
            case 2 -> SoundEvents.NOTE_BLOCK_BELL.value();
            case 3 -> SoundEvents.NOTE_BLOCK_FLUTE.value();
            case 4 -> SoundEvents.NOTE_BLOCK_CHIME.value();
            case 5 -> SoundEvents.NOTE_BLOCK_XYLOPHONE.value();
            default -> SoundEvents.NOTE_BLOCK_HARP.value();
        };
        float pitch = (float) Math.pow(2.0, (random.nextInt(12) - 6) / 12.0); // True musical scale semitone pitch!
        serverWorld.playSound(
                null,
                chicken.getX(), chicken.getY(), chicken.getZ(),
                sound,
                net.minecraft.sounds.SoundSource.RECORDS,
                1.2f, pitch
        );

        // Spawn colorful note particles
        serverWorld.sendParticles(
                ParticleTypes.NOTE,
                chicken.getX(), chicken.getY() + 0.5, chicken.getZ(),
                8,
                0.5, 0.5, 0.5,
                1.0
        );

        // Dye nearby sheep continuously in range
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
