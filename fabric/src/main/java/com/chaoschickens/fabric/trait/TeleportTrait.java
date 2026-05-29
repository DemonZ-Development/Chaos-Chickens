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
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
    public void onApply(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Text.literal("Teleport Chicken").formatted(Formatting.DARK_PURPLE));
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.getWorld() instanceof ServerWorld serverWorld)) return;

        // Spawn ender particles before teleport
        if (com.chaoschickens.fabric.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            serverWorld.spawnParticles(
                    ParticleTypes.PORTAL,
                    chicken.getX(), chicken.getY() + 0.5, chicken.getZ(),
                    15, 0.5, 0.5, 0.5, 0.5
            );
        }

        // Calculate random teleport position
        var random = chicken.getRandom();
        double offsetX = (random.nextDouble() - 0.5) * 2.0 * TELEPORT_RANGE;
        double offsetY = random.nextDouble() * 4.0 - 2.0; // -2 to +2, both directions
        double offsetZ = (random.nextDouble() - 0.5) * 2.0 * TELEPORT_RANGE;

        double newX = chicken.getX() + offsetX;
        double newY = Math.max(chicken.getY() + offsetY - 2.0, serverWorld.getBottomY());
        double newZ = chicken.getZ() + offsetZ;

        // Verify the target chunk is loaded before teleporting
        net.minecraft.util.math.BlockPos targetPos = net.minecraft.util.math.BlockPos.ofFloored(newX, newY, newZ);
        if (!serverWorld.isChunkLoaded(targetPos.getX() >> 4, targetPos.getZ() >> 4)) return;

        // Find a safe landing position (try to land on solid ground)
        for (int i = 0; i < 10; i++) {
            net.minecraft.util.math.BlockPos checkPos = targetPos.down(i);
            if (!serverWorld.getBlockState(checkPos).isAir()
                    && serverWorld.getBlockState(checkPos.up()).isAir()
                    && serverWorld.getBlockState(checkPos.up(2)).isAir()) {
                newY = checkPos.up().getY();
                break;
            }
        }

        // Teleport the chicken
        chicken.setPosition(newX, newY, newZ);

        // Play teleport sound
        serverWorld.playSound(
                null, newX, newY, newZ,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                net.minecraft.sound.SoundCategory.NEUTRAL,
                0.5f, 1.0f
        );

        // Spawn ender particles at new position
        if (com.chaoschickens.fabric.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            serverWorld.spawnParticles(
                    ParticleTypes.PORTAL,
                    newX, newY + 0.5, newZ,
                    15, 0.5, 0.5, 0.5, 0.5
            );
        }
    }
}
