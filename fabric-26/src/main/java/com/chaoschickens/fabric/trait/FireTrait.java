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
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

/**
 * Fire chicken trait.
 * Sets nearby players on fire and periodically spawns flame particles.
 * The fire chicken is IMMUNE to fire and lava - it continuously extinguishes itself.
 */
public class FireTrait extends FabricTrait {

    /** Proximity range for igniting players. */
    private static final double PROXIMITY_RANGE = 5.0;

    /** Duration in ticks to set the player on fire (3 seconds = 60 ticks). */
    private static final int FIRE_DURATION = 60;

    public FireTrait() {
        super(TraitType.FIRE, "A fiery chicken that sets nearby players ablaze!", 1.0,
                true, true, 10); // Tick every server cycle to keep fire immunity active
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Component.literal("Fire Chicken").withStyle(ChatFormatting.GOLD));
        chicken.setCustomNameVisible(true);

        // Make the chicken fire-immune by clearing any fire ticks
        chicken.clearFire();
        chicken.setRemainingFireTicks(-1);
        chicken.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 200, 0, false, false));
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.level() instanceof ServerLevel serverWorld)) return;

        // Refresh fire resistance on every tick call (ticked every 10 ticks, 200 tick duration is a safe refresh)
        chicken.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 200, 0, false, false));

        // === CRITICAL: Continuously suppress fire/lava damage ===
        // Clear fire ticks every tick so lava/fire never kills the chicken
        if (chicken.isOnFire() || chicken.getRemainingFireTicks() > 0) {
            chicken.clearFire();
            chicken.setRemainingFireTicks(-1);
        }

        // If in lava, heal the chicken slowly (fire chickens THRIVE in lava)
        if (chicken.isInLava()) {
            chicken.clearFire();
            chicken.setRemainingFireTicks(-1);
            // Heal 0.5 hearts per tick when in lava
            if (chicken.getHealth() < chicken.getMaxHealth()) {
                chicken.heal(0.5f);
            }
        }

        // Spawn flame particles around the chicken (if enabled)
        if (com.chaoschickens.fabric.util.ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            serverWorld.sendParticles(
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
            serverWorld.sendParticles(
                    ParticleTypes.LAVA,
                    chicken.getX(),
                    chicken.getY() + 0.3,
                    chicken.getZ(),
                    3,
                    0.2, 0.1, 0.2, 0.0
            );
        }

        // Leaves fire while walking!
        if (chicken.onGround()) {
            net.minecraft.core.BlockPos pos = chicken.blockPosition();
            net.minecraft.world.level.block.state.BlockState belowState = serverWorld.getBlockState(pos.below());
            if (serverWorld.getBlockState(pos).isAir() && belowState.isSolid() && !belowState.is(net.minecraft.world.level.block.Blocks.FIRE)) {
                serverWorld.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
            }
        }
    }

    @Override
    public void onPlayerNear(Chicken chicken, Player player) {
        if (player == null || player.isDeadOrDying()) return;
        // Set the player on fire for 3 seconds
        player.setRemainingFireTicks(FIRE_DURATION);
    }

    @Override
    public double getProximityRange() {
        return PROXIMITY_RANGE;
    }
}
