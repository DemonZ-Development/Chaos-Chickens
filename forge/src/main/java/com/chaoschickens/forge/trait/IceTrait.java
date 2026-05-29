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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Ice chicken trait. Freezes water, drops ice, and spawns snowflake particles.
 */
public class IceTrait extends ForgeTrait {

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
        if (!(chicken.level() instanceof ServerLevel serverLevel)) return;

        BlockPos chickenPos = chicken.blockPosition();

        // Freeze water blocks the chicken walks on (skip the chicken's own position)
        for (int dx = -FREEZE_CHECK_RANGE; dx <= FREEZE_CHECK_RANGE; dx++) {
            for (int dz = -FREEZE_CHECK_RANGE; dz <= FREEZE_CHECK_RANGE; dz++) {
                BlockPos checkPos = chickenPos.offset(dx, -1, dz);
                if (checkPos.equals(chickenPos)) continue;
                if (serverLevel.getBlockState(checkPos).is(Blocks.WATER)) {
                    serverLevel.setBlockAndUpdate(checkPos, Blocks.FROSTED_ICE.defaultBlockState());
                }
            }
        }

        // Snowflake particles
        if (chicken.tickCount % 15 == 0 && com.chaoschickens.forge.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                    chicken.getX(), chicken.getY() + 0.5, chicken.getZ(),
                    4, 0.3, 0.3, 0.3, 0.02);
        }
    }

    @Override
    public void onDeath(Chicken chicken, DamageSource source) {
        if (chicken == null) return;
        // Drop ice items
        int count = 1 + chicken.getRandom().nextInt(3);
        chicken.spawnAtLocation(new ItemStack(Items.ICE, count));
    }
}
