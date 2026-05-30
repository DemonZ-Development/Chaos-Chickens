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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;

/**
 * Ice chicken trait.
 * Freezes water blocks the chicken walks on, drops ice on death,
 * and spawns snowflake particles periodically.
 */
public class IceTrait extends FabricTrait {

    /** Range to check for water blocks to freeze. */
    private static final int FREEZE_CHECK_RANGE = 1;

    public IceTrait() {
        super(TraitType.ICE, "A freezing chicken that turns water to ice!", 0.8,
                false, true, 20);
    }

    @Override
    public boolean modifiesDrops() {
        return true;
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Component.literal("Ice Chicken").withStyle(ChatFormatting.AQUA));
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.level() instanceof ServerLevel serverWorld)) return;

        // Freeze water blocks around the chicken (but not the block it's standing in)
        BlockPos chickenPos = chicken.blockPosition();
        for (int dx = -FREEZE_CHECK_RANGE; dx <= FREEZE_CHECK_RANGE; dx++) {
            for (int dz = -FREEZE_CHECK_RANGE; dz <= FREEZE_CHECK_RANGE; dz++) {
                BlockPos checkPos = chickenPos.offset(dx, -1, dz);
                // Skip the block directly under the chicken to prevent self-trapping
                if (checkPos.equals(chickenPos.below())) continue;
                if (serverWorld.getBlockState(checkPos).is(Blocks.WATER)) {
                    serverWorld.setBlockAndUpdate(checkPos, Blocks.FROSTED_ICE.defaultBlockState());
                }
            }
        }

        // Spawn snowflake particles
        if (chicken.tickCount % 15 == 0 && com.chaoschickens.fabric.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            serverWorld.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    chicken.getX(),
                    chicken.getY() + 0.5,
                    chicken.getZ(),
                    4,
                    0.3, 0.3, 0.3,
                    0.02
            );
        }
    }

    @Override
    public void onDeath(Chicken chicken, DamageSource source) {
        if (chicken == null) return;

        // Freeze nearest blocks into ice (2-block radius)
        BlockPos center = chicken.blockPosition();
        net.minecraft.world.level.Level world = chicken.level();
        if (world instanceof ServerLevel serverWorld) {
            int radius = 2;
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = center.offset(x, y, z);
                        net.minecraft.world.level.block.state.BlockState state = world.getBlockState(pos);
                        if (!state.isAir() && !state.is(Blocks.ICE) && !state.is(Blocks.FROSTED_ICE)
                                && !state.is(Blocks.BEDROCK) && !state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BARRIER)) {
                            com.chaoschickens.fabric.ChaosChickensFabric.registerBlockRestore(serverWorld, pos, state, 200);
                            world.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
                        }
                    }
                }
            }
        }

        // Drop ice item
        net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                chicken.level(),
                chicken.getX(),
                chicken.getY(),
                chicken.getZ(),
                new ItemStack(Items.ICE, 1 + chicken.getRandom().nextInt(3)) // 1-3 ice
        );
        chicken.level().addFreshEntity(itemEntity);
    }
}
