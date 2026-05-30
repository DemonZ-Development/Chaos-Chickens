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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

/**
 * Teleport chicken trait.
 * Randomly teleports to a nearby surface position every ~8 seconds,
 * similar to an Enderman's teleport behavior. 8-block horizontal range.
 */
public class TeleportTrait extends FabricTrait {

    /** Maximum teleport distance in blocks (horizontal). */
    private static final double TELEPORT_RANGE = 8.0;

    /** Maximum attempts to find a safe spot. */
    private static final int MAX_ATTEMPTS = 20;

    public TeleportTrait() {
        super(TraitType.TELEPORT, "Randomly teleports around! Hard to catch!", 0.6,
                false, true, 160); // 160 ticks = 8 seconds
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
        if (!(chicken.level() instanceof ServerLevel serverWorld)) return;
        teleport(chicken, serverWorld);
    }

    @Override
    public void onDamage(Chicken chicken, DamageSource source, float amount) {
        if (chicken == null || chicken.isRemoved()) return;
        if (chicken.level() instanceof ServerLevel serverWorld) {
            teleport(chicken, serverWorld);
        }
    }

    public void teleport(Chicken chicken, ServerLevel serverWorld) {
        // Spawn departure particles
        if (com.chaoschickens.fabric.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            serverWorld.sendParticles(
                    ParticleTypes.PORTAL,
                    chicken.getX(), chicken.getY() + 0.5, chicken.getZ(),
                    20, 0.5, 0.5, 0.5, 0.5
            );
        }

        var random = chicken.getRandom();

        // Try multiple attempts to find a safe surface spot
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            // Generate random horizontal offset, guaranteed at least 3 blocks away
            double angle = random.nextDouble() * 2.0 * Math.PI;
            double dist = 3.0 + random.nextDouble() * (TELEPORT_RANGE - 3.0); // 3 to TELEPORT_RANGE blocks
            double offsetX = Math.cos(angle) * dist;
            double offsetZ = Math.sin(angle) * dist;

            double newX = chicken.getX() + offsetX;
            double newZ = chicken.getZ() + offsetZ;

            net.minecraft.core.BlockPos basePos = net.minecraft.core.BlockPos.containing(newX, chicken.getY(), newZ);
            if (!serverWorld.hasChunkAt(basePos)) continue;

            // Search for a safe SURFACE position: scan from current Y upward first, then downward
            // This ensures the chicken always lands on solid ground, not inside blocks
            net.minecraft.core.BlockPos safePos = findSafeSurface(serverWorld, basePos);
            if (safePos == null) continue;

            double finalX = newX;
            double finalY = safePos.getY();
            double finalZ = newZ;

            // Play departure sound
            serverWorld.playSound(
                    null, chicken.getX(), chicken.getY(), chicken.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.NEUTRAL,
                    1.0f, 1.2f
            );

            // Move the chicken
            chicken.teleportTo(finalX, finalY, finalZ);

            // Play arrival sound
            serverWorld.playSound(
                    null, finalX, finalY, finalZ,
                    SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.NEUTRAL,
                    1.0f, 1.0f
            );

            // Spawn arrival particles
            if (com.chaoschickens.fabric.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
                serverWorld.sendParticles(
                        ParticleTypes.PORTAL,
                        finalX, finalY + 0.5, finalZ,
                        20, 0.5, 0.5, 0.5, 0.5
                );
                serverWorld.sendParticles(
                        ParticleTypes.REVERSE_PORTAL,
                        finalX, finalY + 0.5, finalZ,
                        10, 0.3, 0.3, 0.3, 0.2
                );
            }
            return; // Success!
        }
        // If all attempts failed, don't teleport (better than suffocating)
    }

    /**
     * Find a safe surface position near the given block position.
     * Scans Y from +10 to -10 relative to basePos looking for:
     * solid ground with 2 air blocks above.
     */
    private static net.minecraft.core.BlockPos findSafeSurface(ServerLevel world, net.minecraft.core.BlockPos basePos) {
        // Scan from current Y level upward, then downward
        for (int dy = 0; dy <= 10; dy++) {
            // Check upward
            net.minecraft.core.BlockPos checkUp = basePos.offset(0, dy, 0);
            if (isSafeToStand(world, checkUp)) return checkUp.above();

            // Check downward
            if (dy > 0) {
                net.minecraft.core.BlockPos checkDown = basePos.offset(0, -dy, 0);
                if (checkDown.getY() > world.getMinY() && isSafeToStand(world, checkDown)) return checkDown.above();
            }
        }
        return null;
    }

    /**
     * Returns true if the block at pos is solid ground and the two blocks above it are air/passable.
     */
    private static boolean isSafeToStand(ServerLevel world, net.minecraft.core.BlockPos groundPos) {
        var groundState = world.getBlockState(groundPos);
        var aboveState1 = world.getBlockState(groundPos.above());
        var aboveState2 = world.getBlockState(groundPos.above(2));

        return !groundState.isAir()
                && groundState.getFluidState().isEmpty()
                && !groundState.is(net.minecraft.world.level.block.Blocks.LAVA)
                && aboveState1.isAir()
                && aboveState2.isAir();
    }
}
