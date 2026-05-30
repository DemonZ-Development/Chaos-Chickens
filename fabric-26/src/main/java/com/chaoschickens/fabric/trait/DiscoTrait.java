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
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.level() instanceof ServerLevel serverWorld)) return;

        // Play random note block sound
        float pitch = 0.5f + chicken.getRandom().nextFloat() * 1.5f;
        serverWorld.playSound(
                null,
                chicken.getX(), chicken.getY(), chicken.getZ(),
                SoundEvents.NOTE_BLOCK_HARP.value(),
                net.minecraft.sounds.SoundSource.NEUTRAL,
                0.5f, pitch
        );

        // Spawn colorful note particles
        serverWorld.sendParticles(
                ParticleTypes.NOTE,
                chicken.getX(), chicken.getY() + 0.5, chicken.getZ(),
                8,
                0.5, 0.5, 0.5,
                1.0
        );

        // Dye nearby sheep every 2 seconds (30 ticks at interval 15 = every 2 ticks, so gate by tickCount)
        if (chicken.tickCount % 30 == 0) {
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
