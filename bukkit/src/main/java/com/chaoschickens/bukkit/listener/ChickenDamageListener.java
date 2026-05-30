/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.bukkit.listener;

import com.chaoschickens.bukkit.ChaosChickensBukkit;
import com.chaoschickens.bukkit.util.ChickenDataUtil;
import com.chaoschickens.common.trait.TraitType;
import org.bukkit.entity.Chicken;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Listener that cancels fire/lava/explosion damage for fire and boss chickens.
 * Fire chickens are immune to all fire and lava damage.
 * Boss chickens are immune to explosions and fire (if they have fire sub-trait).
 */
public class ChickenDamageListener implements Listener {

    private final ChaosChickensBukkit plugin;

    public ChickenDamageListener(ChaosChickensBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Chicken chicken)) return;

        // Check if this chicken has a chaos trait
        if (!ChickenDataUtil.hasTrait(plugin, chicken)) return;

        TraitType traitType = ChickenDataUtil.getTrait(plugin, chicken);
        if (traitType == TraitType.EMPTY) return;

        EntityDamageEvent.DamageCause cause = event.getCause();

        // Fire chicken: immune to ALL fire and lava damage
        if (traitType == TraitType.FIRE) {
            if (isFireDamage(cause)) {
                event.setCancelled(true);
                chicken.setFireTicks(0);
                return;
            }
        }

        // Boss chicken: immune to explosions and fire (if has fire sub-trait)
        if (traitType == TraitType.BOSS) {
            // Boss is always immune to explosions (its own fireballs, death explosion)
            if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                    || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                event.setCancelled(true);
                return;
            }

            // If boss has fire sub-trait, immune to fire/lava too
            java.util.List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(plugin, chicken);
            if (subTraits.contains(TraitType.FIRE) && isFireDamage(cause)) {
                event.setCancelled(true);
                chicken.setFireTicks(0);
                return;
            }
        }
    }

    private boolean isFireDamage(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.HOT_FLOOR;
    }
}
