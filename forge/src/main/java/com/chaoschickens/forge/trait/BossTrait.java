/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
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
import com.chaoschickens.forge.ChaosChickensForge;
import com.chaoschickens.forge.util.ChickenDataUtil;
import com.chaoschickens.forge.util.ConfigLoader;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import java.util.Random;

public class BossTrait extends ForgeTrait {
    
    private static final Random RANDOM = new Random();

    public BossTrait() {
        super(TraitType.BOSS, "A powerful chicken with multiple chaos traits combined!", 0.02,
                true, true, 20); // Ticked more frequently for sub-traits responsiveness (20 ticks = 1s)
    }
    
    @Override
    public boolean modifiesDrops() {
        return true;
    }

    @Override
    public void onModifyDrops(Chicken chicken, LivingDropsEvent event) {
        java.util.List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(chicken);
        for (TraitType type : subTraits) {
            com.chaoschickens.forge.trait.ForgeTrait trait = ChaosChickensForge.getTraitInstance(type);
            if (trait != null && trait.modifiesDrops()) {
                trait.onModifyDrops(chicken, event);
            }
        }
    }

    @Override
    public void onApply(Chicken chicken) {
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
            com.chaoschickens.forge.trait.ForgeTrait trait = ChaosChickensForge.getTraitInstance(type);
            if (trait != null) {
                trait.onApply(chicken);
            }
        }

        // Set Boss name and health (ensure they take precedence over sub-traits)
        chicken.setCustomName(Component.literal("BOSS Chicken").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        chicken.setCustomNameVisible(true);
        if (chicken.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) != null) {
            chicken.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(12.0);
            chicken.setHealth(12.0f);
        }
    }
    
    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isRemoved() || chicken.isDeadOrDying()) return;

        // Boss aura particles (if enabled)
        if (ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            ((ServerLevel) chicken.level()).sendParticles(ParticleTypes.DRAGON_BREATH,
                chicken.getX(), chicken.getY() + 1, chicken.getZ(),
                1, 0, 0.05, 0, 0);
        }

        // Tick all sub-traits!
        java.util.List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(chicken);
        for (TraitType type : subTraits) {
            com.chaoschickens.forge.trait.ForgeTrait trait = ChaosChickensForge.getTraitInstance(type);
            if (trait != null && trait.isPeriodic()) {
                trait.onTick(chicken);
            }
        }
    }

    @Override
    public void onDeath(Chicken chicken, net.minecraft.world.damagesource.DamageSource source) {
        if (chicken == null) return;

        if (ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            ((ServerLevel) chicken.level()).sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                chicken.getX(), chicken.getY(), chicken.getZ(),
                1, 0, 0, 0, 0);
        }

        // Trigger death for all sub-traits!
        java.util.List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(chicken);
        for (TraitType type : subTraits) {
            com.chaoschickens.forge.trait.ForgeTrait trait = ChaosChickensForge.getTraitInstance(type);
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
    public void onPlayerNear(Chicken chicken, Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));

        // Delegate proximity effects for all sub-traits!
        java.util.List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(chicken);
        for (TraitType type : subTraits) {
            com.chaoschickens.forge.trait.ForgeTrait trait = ChaosChickensForge.getTraitInstance(type);
            if (trait != null && trait.getProximityRange() > 0) {
                double distSq = chicken.distanceToSqr(player);
                if (distSq <= trait.getProximityRange() * trait.getProximityRange()) {
                    trait.onPlayerNear(chicken, player);
                }
            }
        }
    }
}
