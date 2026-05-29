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
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Zombie chicken trait.
 * Hostile chicken that chases nearby players and deals damage on contact.
 */
public class ZombieTrait extends FabricTrait {

    /** Detection range for chasing players. */
    private static final double CHASE_RANGE = 8.0;

    /** Damage range — must be this close to deal damage. */
    private static final double DAMAGE_RANGE = 1.5;

    /** Chase speed multiplier. */
    private static final double CHASE_SPEED = 0.25;

    /** Damage dealt on contact. */
    private static final float CONTACT_DAMAGE = 1.0f;

    public ZombieTrait() {
        super(TraitType.ZOMBIE, "A hostile chicken that chases players!", 0.7,
                true, true, 20);
    }

    @Override
    public void onApply(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Text.literal("Zombie Chicken").formatted(Formatting.DARK_RED));
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.getWorld() instanceof ServerWorld serverWorld)) return;

        // Find nearest player within chase range
        List<PlayerEntity> nearbyPlayers = serverWorld.getEntitiesByClass(
                PlayerEntity.class,
                chicken.getBoundingBox().expand(CHASE_RANGE),
                player -> player.isAlive() && !player.isSpectator() && !player.isCreative()
        );

        if (nearbyPlayers.isEmpty()) return;

        // Find closest player
        PlayerEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (PlayerEntity player : nearbyPlayers) {
            double dist = player.squaredDistanceTo(chicken);
            if (dist < closestDist) {
                closestDist = dist;
                closest = player;
            }
        }

        if (closest == null) return;

        // Chase the player by setting velocity toward them
        Vec3d direction = closest.getPos().subtract(chicken.getPos()).normalize().multiply(CHASE_SPEED);
        chicken.setVelocity(direction);
        chicken.velocityModified = true;

        // Deal damage if very close
        if (closest.squaredDistanceTo(chicken) <= DAMAGE_RANGE * DAMAGE_RANGE) {
            closest.damage(chicken.getDamageSources().mobAttack(chicken), CONTACT_DAMAGE);
            closest.playSound(SoundEvents.ENTITY_ZOMBIE_AMBIENT, 0.5f, 1.5f);
        }

        // Spawn angry particles
        if (chicken.age % 10 == 0) {
            serverWorld.spawnParticles(
                    ParticleTypes.ANGRY_VILLAGER,
                    chicken.getX(), chicken.getY() + 0.8, chicken.getZ(),
                    2, 0.3, 0.3, 0.3, 0.02
            );
        }
    }
}
