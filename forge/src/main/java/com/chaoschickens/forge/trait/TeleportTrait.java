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
import net.minecraft.world.phys.Vec3;

/**
 * Teleport chicken trait. Randomly teleports every ~10 seconds.
 */
public class TeleportTrait extends ForgeTrait {

    private static final double TELEPORT_RANGE = 15.0;

    public TeleportTrait() {
        super(TraitType.TELEPORT, "Randomly teleports around! Hard to catch!", 0.6,
                false, true, 200);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Component.literal("Teleport Chicken").withStyle(ChatFormatting.DARK_PURPLE));
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.level() instanceof ServerLevel serverLevel)) return;

        // Pre-teleport particles
        if (com.chaoschickens.forge.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    chicken.getX(), chicken.getY() + 0.5, chicken.getZ(),
                    15, 0.5, 0.5, 0.5, 0.5);
        }

        // Calculate random teleport position
        var random = chicken.getRandom();
        double offsetX = (random.nextDouble() - 0.5) * 2.0 * TELEPORT_RANGE;
        double offsetY = random.nextDouble() * 4.0 - 2.0;
        double offsetZ = (random.nextDouble() - 0.5) * 2.0 * TELEPORT_RANGE;

        double newX = chicken.getX() + offsetX;
        double newY = chicken.getY() + offsetY;
        double newZ = chicken.getZ() + offsetZ;

        // Find safe landing position
        var targetPos = net.minecraft.core.BlockPos.containing(newX, newY, newZ);
        for (int i = 0; i < 10; i++) {
            var checkPos = targetPos.below(i);
            if (!serverLevel.getBlockState(checkPos).isAir()
                    && serverLevel.getBlockState(checkPos.above()).isAir()
                    && serverLevel.getBlockState(checkPos.above(2)).isAir()) {
                newY = checkPos.above().getY();
                break;
            }
        }

        // Teleport
        chicken.teleportTo(newX, newY, newZ);

        // Sound and particles at new position
        serverLevel.playSound(null, newX, newY, newZ,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.5f, 1.0f);
        if (com.chaoschickens.forge.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    newX, newY + 0.5, newZ,
                    15, 0.5, 0.5, 0.5, 0.5);
        }
    }
}
