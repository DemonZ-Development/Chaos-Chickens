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
import net.minecraft.block.BlockState;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * Teleport chicken trait.
 * Randomly teleports to a nearby surface position every ~8 seconds,
 * similar to an Enderman's teleport behavior. 8-block horizontal range.
 * Has 16.0 max health (8 hearts) and teleports on damage.
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
    public void onApply(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Text.literal("Teleport Chicken").formatted(Formatting.DARK_PURPLE));
        chicken.setCustomNameVisible(true);

        // Give 8 hearts (16.0 HP)
        var maxHealthAttr = chicken.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(16.0);
            chicken.setHealth(16.0f);
        }
    }

    @Override
    public void onTick(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.getWorld() instanceof ServerWorld serverWorld)) return;
        teleport(chicken, serverWorld);
    }

    @Override
    public void onDamage(ChickenEntity chicken, DamageSource source, float amount) {
        if (chicken == null || chicken.isRemoved()) return;
        if (chicken.getWorld() instanceof ServerWorld serverWorld) {
            teleport(chicken, serverWorld);
        }
    }

    /**
     * Teleports the chicken to a safe nearby position.
     */
    public void teleport(ChickenEntity chicken, ServerWorld serverWorld) {
        // Spawn departure particles
        if (com.chaoschickens.fabric.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            serverWorld.spawnParticles(
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

            BlockPos basePos = BlockPos.ofFloored(newX, chicken.getY(), newZ);
            if (!serverWorld.isChunkLoaded(basePos.getX() >> 4, basePos.getZ() >> 4)) continue;

            // Search for a safe SURFACE position: scan from current Y upward first, then downward
            BlockPos safePos = findSafeSurface(serverWorld, basePos);
            if (safePos == null) continue;

            double finalX = newX;
            double finalY = safePos.getY();
            double finalZ = newZ;

            // Play departure sound
            serverWorld.playSound(
                    null, chicken.getX(), chicken.getY(), chicken.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    SoundCategory.NEUTRAL,
                    1.0f, 1.2f
            );

            // Move the chicken
            chicken.setPosition(finalX, finalY, finalZ);

            // Play arrival sound
            serverWorld.playSound(
                    null, finalX, finalY, finalZ,
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    SoundCategory.NEUTRAL,
                    1.0f, 1.0f
            );

            // Spawn arrival particles
            if (com.chaoschickens.fabric.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
                serverWorld.spawnParticles(
                        ParticleTypes.PORTAL,
                        finalX, finalY + 0.5, finalZ,
                        20, 0.5, 0.5, 0.5, 0.5
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
    private static BlockPos findSafeSurface(ServerWorld world, BlockPos basePos) {
        // Scan from current Y level upward, then downward
        for (int dy = 0; dy <= 10; dy++) {
            // Check upward
            BlockPos checkUp = basePos.add(0, dy, 0);
            if (isSafeToStand(world, checkUp)) return checkUp.up();

            // Check downward
            if (dy > 0) {
                BlockPos checkDown = basePos.add(0, -dy, 0);
                if (checkDown.getY() > world.getBottomY() && isSafeToStand(world, checkDown)) return checkDown.up();
            }
        }
        return null;
    }

    /**
     * Returns true if the block at pos is solid ground and the two blocks above it are air.
     */
    private static boolean isSafeToStand(ServerWorld world, BlockPos groundPos) {
        BlockState groundState = world.getBlockState(groundPos);
        BlockState aboveState1 = world.getBlockState(groundPos.up());
        BlockState aboveState2 = world.getBlockState(groundPos.up(2));

        return !groundState.isAir()
                && groundState.getFluidState().isEmpty()
                && !groundState.isOf(net.minecraft.block.Blocks.LAVA)
                && !groundState.isOf(net.minecraft.block.Blocks.FIRE)
                && !groundState.isOf(net.minecraft.block.Blocks.SOUL_FIRE)
                && !groundState.isOf(net.minecraft.block.Blocks.CACTUS)
                && aboveState1.isAir()
                && aboveState2.isAir();
    }
}
