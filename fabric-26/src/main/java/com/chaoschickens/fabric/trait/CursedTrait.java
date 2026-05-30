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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

/**
 * Cursed chicken trait.
 * Applies random negative status effects to nearby players.
 */
public class CursedTrait extends FabricTrait {

    /** Proximity range for cursing players. */
    private static final double PROXIMITY_RANGE = 6.0;

    /** Effect durations in ticks. */
    private static final int LONG_DURATION = 200;  // 10 seconds
    private static final int SHORT_DURATION = 100;  // 5 seconds

    public CursedTrait() {
        super(TraitType.CURSED, "Curses nearby players with random bad effects!", 0.7,
                true, true, 40);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Component.literal("Cursed Chicken").withStyle(ChatFormatting.DARK_PURPLE));
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.level() instanceof ServerLevel serverWorld)) return;

        // Only spawn particles when players are nearby
        boolean playersNearby = !serverWorld.players().stream()
                .filter(player -> !player.isDeadOrDying())
                .filter(player -> player.distanceToSqr(chicken) <= PROXIMITY_RANGE * PROXIMITY_RANGE)
                .toList()
                .isEmpty();
        if (playersNearby) {
            serverWorld.sendParticles(
                    ParticleTypes.WITCH,
                    chicken.getX(), chicken.getY() + 0.5, chicken.getZ(),
                    4, 0.3, 0.3, 0.3, 0.02
            );
        }
    }

    @Override
    public void onPlayerNear(Chicken chicken, Player player) {
        if (chicken == null || chicken.getRandom() == null) return;
        if (player == null || player.isDeadOrDying()) return;

        var random = chicken.getRandom();
        int effectIndex = random.nextInt(5);
        MobEffectInstance effect = switch (effectIndex) {
            case 0 -> new MobEffectInstance(MobEffects.POISON, LONG_DURATION, 0);
            case 1 -> new MobEffectInstance(MobEffects.SLOWNESS, LONG_DURATION, 0);
            case 2 -> new MobEffectInstance(MobEffects.WEAKNESS, LONG_DURATION, 0);
            case 3 -> new MobEffectInstance(MobEffects.BLINDNESS, SHORT_DURATION, 0);
            case 4 -> new MobEffectInstance(MobEffects.WITHER, SHORT_DURATION, 0);
            default -> new MobEffectInstance(MobEffects.POISON, LONG_DURATION, 0);
        };

        player.addEffect(effect);
    }

    @Override
    public double getProximityRange() {
        return PROXIMITY_RANGE;
    }
}
