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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

/**
 * Teleport chicken trait.
 * Randomly teleports to a nearby position every ~10 seconds,
 * similar to an Enderman's teleport behavior.
 */
public class TeleportTrait extends FabricTrait {

    /** Maximum teleport distance in blocks. */
    private static final double TELEPORT_RANGE = 15.0;

    /** Minimum teleport distance to avoid teleporting to the same spot. */
    private static final double MIN_TELEPORT_DISTANCE = 3.0;

    public TeleportTrait() {
        super(TraitType.TELEPORT, "Randomly teleports around! Hard to catch!", 0.6,
                false, true, 200); // 200 ticks = 10 seconds
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
        if (!(chicken.level() instanceof ServerLevel serverWorld)) return;

        // Spawn ender particles before teleport
        if (com.chaoschickens.fabric.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            serverWorld.sendParticles(
                    ParticleTypes.PORTAL,
                    chicken.getX(), chicken.getY() + 0.5, chicken.getZ(),
                    15, 0.5, 0.5, 0.5, 0.5
            );
        }

        // Calculate random teleport position
        var random = chicken.getRandom();
        double offsetX = (random.nextDouble() - 0.5) * 2.0 * TELEPORT_RANGE;
        double offsetY = random.nextDouble() * 4.0 - 2.0;
        double offsetZ = (random.nextDouble() - 0.5) * 2.0 * TELEPORT_RANGE;

        double newX = chicken.getX() + offsetX;
        double newY = Math.max(chicken.getY() + offsetY - 2.0, serverWorld.getMinY());
        double newZ = chicken.getZ() + offsetZ;

        // Verify the target chunk is loaded before teleporting
        net.minecraft.core.BlockPos targetPos = net.minecraft.core.BlockPos.containing(newX, newY, newZ);
        if (!serverWorld.hasChunkAt(targetPos)) return;

        // Find a safe landing position (try to land on solid ground)
        for (int i = 0; i < 10; i++) {
            net.minecraft.core.BlockPos checkPos = targetPos.below(i);
            if (!serverWorld.getBlockState(checkPos).isAir()
                    && serverWorld.getBlockState(checkPos.above()).isAir()
                    && serverWorld.getBlockState(checkPos.above(2)).isAir()) {
                newY = checkPos.above().getY();
                break;
            }
        }

        // Teleport the chicken
        chicken.setPos(newX, newY, newZ);

        // Play teleport sound
        serverWorld.playSound(
                null, newX, newY, newZ,
                SoundEvents.ENDERMAN_TELEPORT,
                net.minecraft.sounds.SoundSource.NEUTRAL,
                0.5f, 1.0f
        );

        // Spawn ender particles at new position
        if (com.chaoschickens.fabric.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            serverWorld.sendParticles(
                    ParticleTypes.PORTAL,
                    newX, newY + 0.5, newZ,
                    15, 0.5, 0.5, 0.5, 0.5
            );
        }
    }
}
