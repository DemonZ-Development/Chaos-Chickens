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
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * Fire chicken trait.
 * Sets nearby players on fire, leaves a trail of fire, spawns flame/lava particles,
 * and is completely immune to fire and lava damage (healing when in lava).
 */
public class FireTrait extends FabricTrait {

    /** Proximity range for igniting players. */
    private static final double PROXIMITY_RANGE = 5.0;

    /** Duration in ticks to set the player on fire (3 seconds = 60 ticks). */
    private static final int FIRE_DURATION = 60;

    public FireTrait() {
        super(TraitType.FIRE, "A fiery chicken that sets nearby players ablaze!", 1.0,
                true, true, 10); // Ticks every 10 ticks (0.5s) for responsive behaviors
    }

    @Override
    public void onApply(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Text.literal("Fire Chicken").formatted(Formatting.GOLD));
        chicken.setCustomNameVisible(true);

        // Immediately extinguish and apply fire resistance
        chicken.extinguish();
        chicken.setFireTicks(-1);
        chicken.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 200, 0, false, false));
    }

    @Override
    public void onTick(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.getWorld() instanceof ServerWorld serverWorld)) return;

        // Continuously refresh fire resistance potion effect on every tick call (ticked every 10 ticks, 200 tick duration is a safe refresh)
        chicken.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 200, 0, false, false));

        // Extinguish fire ticks
        if (chicken.isOnFire() || chicken.getFireTicks() > 0) {
            chicken.extinguish();
        }

        // Heal in lava
        if (chicken.isInLava()) {
            chicken.extinguish();
            if (chicken.getHealth() < chicken.getMaxHealth()) {
                chicken.heal(0.5f);
            }
        }

        // Spawn flame particles around the chicken (if enabled)
        if (com.chaoschickens.fabric.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            serverWorld.spawnParticles(
                    ParticleTypes.FLAME,
                    chicken.getX(),
                    chicken.getY() + 0.5,
                    chicken.getZ(),
                    5, // count
                    0.3, // deltaX
                    0.3, // deltaY
                    0.3, // deltaZ
                    0.02 // speed
            );
        }

        // Lava swimming particles
        if (chicken.isInLava() && com.chaoschickens.fabric.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            serverWorld.spawnParticles(
                    ParticleTypes.LAVA,
                    chicken.getX(),
                    chicken.getY() + 0.3,
                    chicken.getZ(),
                    3,
                    0.2, 0.1, 0.2, 0.0
            );
        }

        // Leaves fire while walking!
        if (chicken.isOnGround()) {
            BlockPos pos = chicken.getBlockPos();
            BlockState belowState = serverWorld.getBlockState(pos.down());
            if (serverWorld.getBlockState(pos).isAir() && !belowState.isAir() && belowState.getFluidState().isEmpty() && !belowState.isOf(Blocks.FIRE)) {
                serverWorld.setBlockState(pos, Blocks.FIRE.getDefaultState());
            }
        }
    }

    @Override
    public void onPlayerNear(ChickenEntity chicken, PlayerEntity player) {
        if (player == null || player.isDead()) return;
        // Set the player on fire for 3 seconds
        player.setFireTicks(FIRE_DURATION);
    }

    @Override
    public double getProximityRange() {
        return PROXIMITY_RANGE;
    }
}
