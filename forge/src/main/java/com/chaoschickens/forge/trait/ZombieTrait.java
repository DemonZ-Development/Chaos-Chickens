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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Zombie chicken trait. Hostile — chases players and deals damage on contact.
 */
public class ZombieTrait extends ForgeTrait {

    private static final double CHASE_RANGE = 8.0;
    private static final double DAMAGE_RANGE = 1.5;
    private static final double CHASE_SPEED = 0.10;
    private static final float CONTACT_DAMAGE = 4.0f;

    public ZombieTrait() {
        super(TraitType.ZOMBIE, "A hostile chicken that chases players!", 0.7,
                true, true, 1);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Component.literal("Zombie Chicken").withStyle(ChatFormatting.GREEN));
        chicken.setCustomNameVisible(true);
        if (chicken.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) != null) {
            chicken.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20.0);
            chicken.setHealth(20.0f);
        }
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.level() instanceof ServerLevel serverLevel)) return;

        List<Player> nearbyPlayers = serverLevel.getEntitiesOfClass(
                Player.class, chicken.getBoundingBox().inflate(CHASE_RANGE),
                player -> player.isAlive() && !player.isSpectator() && !player.isCreative());

        if (nearbyPlayers.isEmpty()) return;

        Player closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Player player : nearbyPlayers) {
            double dist = player.distanceToSqr(chicken);
            if (dist < closestDist) {
                closestDist = dist;
                closest = player;
            }
        }
        if (closest == null) return;

        // Chase the player using pathfinding
        if (chicken.tickCount % 5 == 0) {
            chicken.getNavigation().moveTo(closest, 1.25);
        }

        // Deal damage if close enough
        if (closest.distanceToSqr(chicken) <= DAMAGE_RANGE * DAMAGE_RANGE) {
            closest.hurt(chicken.damageSources().mobAttack(chicken), CONTACT_DAMAGE);
            closest.playSound(SoundEvents.ZOMBIE_AMBIENT, 0.5f, 1.5f);
        }

        // Angry particles every 5 ticks to avoid visual clutter
        if (chicken.tickCount % 5 == 0) {
            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    chicken.getX(), chicken.getY() + 0.8, chicken.getZ(),
                    2, 0.3, 0.3, 0.3, 0.02);
        }
    }
}
