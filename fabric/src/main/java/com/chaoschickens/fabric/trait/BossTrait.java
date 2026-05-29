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
import com.chaoschickens.fabric.ChaosChickensFabric;
import com.chaoschickens.fabric.util.ChickenDataUtil;
import com.chaoschickens.fabric.util.ConfigLoader;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Random;

public class BossTrait extends FabricTrait {

    private static final Random RANDOM = new Random();

    public BossTrait() {
        super(TraitType.BOSS, "A powerful chicken with multiple chaos traits combined!", 0.02,
                true, true, 20); // Ticked more frequently for sub-traits responsiveness (20 ticks = 1s)
    }

    @Override
    public void onApply(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;

        // Load or roll sub-traits
        java.util.List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(chicken);
        if (subTraits.isEmpty()) {
            int count = ConfigLoader.getConfig().getBossTraitCount();
            java.util.List<TraitType> available = new java.util.ArrayList<>();
            for (TraitType t : TraitType.values()) {
                if (t != TraitType.EMPTY && t != TraitType.BOSS && t != TraitType.CUSTOM) {
                    if (com.chaoschickens.api.ChaosChickensAPI.isTraitEnabled(t) &&
                        com.chaoschickens.api.ChaosChickensAPI.getTraitWeight(t) > 0.0) {
                        available.add(t);
                    }
                }
            }
            java.util.Collections.shuffle(available);
            for (int i = 0; i < Math.min(count, available.size()); i++) {
                subTraits.add(available.get(i));
            }
            ChickenDataUtil.setBossSubTraits(chicken, subTraits);
        }

        // Apply all sub-traits!
        for (TraitType type : subTraits) {
            com.chaoschickens.fabric.trait.FabricTrait trait = ChaosChickensFabric.getTraitInstance(type);
            if (trait != null) {
                trait.onApply(chicken);
            }
        }

        // Set Boss name and health (ensure they take precedence over sub-traits)
        chicken.setCustomName(Text.literal("BOSS Chicken").formatted(Formatting.DARK_RED, Formatting.BOLD));
        chicken.setCustomNameVisible(true);
        if (chicken.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH) != null) {
            chicken.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(12.0);
            chicken.setHealth(12.0f);
        }
    }

    @Override
    public void onTick(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved() || chicken.isDead()) return;

        // Boss aura particles (if enabled)
        if (ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            ((ServerWorld) chicken.getWorld()).spawnParticles(ParticleTypes.DRAGON_BREATH,
                chicken.getX(), chicken.getY() + 1, chicken.getZ(),
                1, 0, 0.05, 0, 0);
        }

        // Tick all sub-traits!
        java.util.List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(chicken);
        for (TraitType type : subTraits) {
            com.chaoschickens.fabric.trait.FabricTrait trait = ChaosChickensFabric.getTraitInstance(type);
            if (trait != null && trait.isPeriodic()) {
                trait.onTick(chicken);
            }
        }
    }

    @Override
    public void onDeath(ChickenEntity chicken, net.minecraft.entity.damage.DamageSource source) {
        if (chicken == null) return;

        if (ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            ((ServerWorld) chicken.getWorld()).spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                chicken.getX(), chicken.getY(), chicken.getZ(),
                1, 0, 0, 0, 0);
        }

        // Trigger death for all sub-traits!
        java.util.List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(chicken);
        for (TraitType type : subTraits) {
            com.chaoschickens.fabric.trait.FabricTrait trait = ChaosChickensFabric.getTraitInstance(type);
            if (trait != null) {
                trait.onDeath(chicken, source);
            }
        }
    }

    @Override
    public double getProximityRange() {
        return 16.0;
    }

    @Override
    public void onPlayerNear(ChickenEntity chicken, PlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 0));

        // Delegate proximity effects for all sub-traits!
        java.util.List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(chicken);
        for (TraitType type : subTraits) {
            com.chaoschickens.fabric.trait.FabricTrait trait = ChaosChickensFabric.getTraitInstance(type);
            if (trait != null && trait.getProximityRange() > 0) {
                double distSq = chicken.squaredDistanceTo(player);
                if (distSq <= trait.getProximityRange() * trait.getProximityRange()) {
                    trait.onPlayerNear(chicken, player);
                }
            }
        }
    }
}
