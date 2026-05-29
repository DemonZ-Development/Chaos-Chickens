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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Fire chicken trait.
 * Sets nearby players on fire and periodically spawns flame particles.
 */
public class FireTrait extends FabricTrait {

    /** Proximity range for igniting players. */
    private static final double PROXIMITY_RANGE = 5.0;

    /** Duration in ticks to set the player on fire (3 seconds = 60 ticks). */
    private static final int FIRE_DURATION = 60;

    public FireTrait() {
        super(TraitType.FIRE, "A fiery chicken that sets nearby players ablaze!", 1.0,
                true, true, 40);
    }

    @Override
    public void onApply(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Text.literal("Fire Chicken").formatted(Formatting.GOLD));
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.getWorld() instanceof ServerWorld serverWorld)) return;

        // Spawn flame particles around the chicken (if enabled)
        if (chicken.age % 10 == 0 && com.chaoschickens.fabric.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
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
