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
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Teleport chicken trait.
 * Teleports like an enderman: 3-8 block range, safe surface detection,
 * no suffocation, no lava landing.
 */
public class TeleportTrait extends ForgeTrait {

    private static final double MIN_RANGE = 3.0;
    private static final double MAX_RANGE = 8.0;
    private static final int MAX_ATTEMPTS = 20;

    public TeleportTrait() {
        super(TraitType.TELEPORT, "Randomly teleports around! Hard to catch!", 0.6,
                false, true, 160); // Every 8 seconds
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Component.literal("Teleport Chicken").withStyle(ChatFormatting.DARK_PURPLE));
        chicken.setCustomNameVisible(true);
        if (chicken.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) != null) {
            chicken.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(16.0);
            chicken.setHealth(16.0f);
        }
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.level() instanceof ServerLevel serverLevel)) return;
        teleport(chicken, serverLevel);
    }

    /**
     * Perform a safe teleport for the chicken. Public so BossTrait can call it.
     */
    public void teleport(Chicken chicken, ServerLevel serverLevel) {
        if (chicken == null || chicken.isRemoved()) return;

        var random = chicken.getRandom();

        // Pre-teleport particles
        if (com.chaoschickens.forge.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    chicken.getX(), chicken.getY() + 0.5, chicken.getZ(),
                    20, 0.3, 0.5, 0.3, 0.5);
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    chicken.getX(), chicken.getY() + 0.5, chicken.getZ(),
                    10, 0.2, 0.3, 0.2, 0.3);
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            // Polar coordinates for enderman-like spread
            double angle = random.nextDouble() * 2.0 * Math.PI;
            double distance = MIN_RANGE + random.nextDouble() * (MAX_RANGE - MIN_RANGE);

            double newX = chicken.getX() + Math.cos(angle) * distance;
            double newZ = chicken.getZ() + Math.sin(angle) * distance;
            double baseY = chicken.getY();

            // Scan for safe surface (±10 Y)
            boolean found = false;
            double newY = baseY;

            for (int dy = 0; dy <= 10; dy++) {
                for (int sign : new int[]{1, -1}) {
                    double checkY = baseY + (dy * sign);
                    if (checkY < serverLevel.getMinBuildHeight() || checkY > serverLevel.getMaxBuildHeight() - 2) continue;

                    BlockPos ground = BlockPos.containing(newX, checkY - 1, newZ);
                    BlockPos feet = ground.above();
                    BlockPos head = feet.above();

                    BlockState groundState = serverLevel.getBlockState(ground);
                    BlockState feetState = serverLevel.getBlockState(feet);
                    BlockState headState = serverLevel.getBlockState(head);

                    // Ground must be solid, feet + head must be air, no lava/fire/cactus
                    if (groundState.isSolid()
                            && feetState.isAir()
                            && headState.isAir()
                            && !groundState.is(Blocks.LAVA)
                            && !groundState.is(Blocks.FIRE)
                            && !groundState.is(Blocks.SOUL_FIRE)
                            && !groundState.is(Blocks.CACTUS)
                            && !groundState.is(Blocks.MAGMA_BLOCK)) {
                        newY = feet.getY();
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }

            if (found) {
                chicken.teleportTo(newX, newY, newZ);

                // Post-teleport effects
                serverLevel.playSound(null, newX, newY, newZ,
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.5f, 1.2f);

                if (com.chaoschickens.forge.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
                    serverLevel.sendParticles(ParticleTypes.PORTAL,
                            newX, newY + 0.5, newZ,
                            20, 0.3, 0.5, 0.3, 0.5);
                    serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                            newX, newY + 0.5, newZ,
                            10, 0.2, 0.3, 0.2, 0.3);
                }
                return; // Success
            }
        }
        // All attempts failed — chicken stays put
    }
}
