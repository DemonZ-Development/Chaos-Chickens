/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.bukkit.trait;

import com.chaoschickens.bukkit.util.ChickenDataUtil;
import com.chaoschickens.common.trait.TraitType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.Particle;

import java.util.Random;

public class BossTrait extends BukkitTrait {
    
    public BossTrait() {
        super(TraitType.BOSS, "A powerful chicken with multiple chaos traits combined!", 0.02,
                true, true, 20); // Ticked more frequently for sub-traits responsiveness (20 ticks = 1s)
    }
    
    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;

        // Load or roll sub-traits
        java.util.List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(plugin, chicken);
        if (subTraits.isEmpty()) {
            int count = plugin.getConfigManager().getBossTraitCount();
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
            ChickenDataUtil.setBossSubTraits(plugin, chicken, subTraits);
        }

        // Apply all sub-traits!
        for (TraitType type : subTraits) {
            com.chaoschickens.bukkit.trait.BukkitTrait trait = plugin.getTraitMap().get(type);
            if (trait != null) {
                trait.onApply(chicken);
            }
        }

        // Set Boss name and health (ensure they take precedence over sub-traits)
        chicken.setCustomName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "BOSS Chicken");
        chicken.setCustomNameVisible(true);
        chicken.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(12.0);
        chicken.setHealth(12.0);
    }
    
    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        
        // Boss aura particles (if enabled)
        if (plugin.getConfigManager().isTraitParticlesEnabled()) {
            chicken.getWorld().spawnParticle(Particle.DRAGON_BREATH, 
                chicken.getLocation().clone().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.02);
        }

        // Tick all sub-traits!
        java.util.List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(plugin, chicken);
        for (TraitType type : subTraits) {
            com.chaoschickens.bukkit.trait.BukkitTrait trait = plugin.getTraitMap().get(type);
            if (trait != null && trait.isPeriodic()) {
                trait.onTick(chicken);
            }
        }
    }
    
    @Override
    public void onDeath(Chicken chicken, EntityDeathEvent event) {
        if (chicken == null) return;
        
        if (plugin.getConfigManager().isTraitParticlesEnabled()) {
            Particle explosionParticle;
            try {
                explosionParticle = Particle.valueOf("EXPLOSION_EMITTER");
            } catch (IllegalArgumentException e) {
                try {
                    explosionParticle = Particle.valueOf("EXPLOSION_HUGE");
                } catch (IllegalArgumentException e2) {
                    explosionParticle = Particle.valueOf("EXPLOSION_NORMAL");
                }
            }
            chicken.getWorld().spawnParticle(explosionParticle,
                chicken.getLocation(), 3, 0.5, 0.5, 0.5, 0);
            chicken.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                chicken.getLocation(), 30, 1, 1, 1, 0.05);
        }

        // Trigger death for all sub-traits!
        java.util.List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(plugin, chicken);
        for (TraitType type : subTraits) {
            com.chaoschickens.bukkit.trait.BukkitTrait trait = plugin.getTraitMap().get(type);
            if (trait != null) {
                trait.onDeath(chicken, event);
            }
        }
    }
    
    @Override
    public double getProximityRange() {
        return 16.0;
    }
    
    @Override
    public void onPlayerNear(Chicken chicken, Player player) {
        // Boss chickens have a menacing aura that gives players weakness
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.WEAKNESS, 40, 0));

        // Delegate proximity effects for all sub-traits!
        java.util.List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(plugin, chicken);
        for (TraitType type : subTraits) {
            com.chaoschickens.bukkit.trait.BukkitTrait trait = plugin.getTraitMap().get(type);
            if (trait != null && trait.getProximityRange() > 0) {
                double dist = chicken.getLocation().distance(player.getLocation());
                if (dist <= trait.getProximityRange()) {
                    trait.onPlayerNear(chicken, player);
                }
            }
        }
    }
}
