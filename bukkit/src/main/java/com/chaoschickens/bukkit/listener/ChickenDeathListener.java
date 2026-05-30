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
import com.chaoschickens.bukkit.trait.BukkitTrait;
import com.chaoschickens.bukkit.util.ChickenDataUtil;
import com.chaoschickens.common.trait.TraitType;
import org.bukkit.entity.Chicken;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Listener that handles chicken death events.
 * Triggers trait-specific death behavior like explosions and modified drops.
 */
public class ChickenDeathListener implements Listener {

    private final ChaosChickensBukkit plugin;

    public ChickenDeathListener(ChaosChickensBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        // Only handle chicken deaths
        if (!(event.getEntity() instanceof Chicken)) return;

        Chicken chicken = (Chicken) event.getEntity();

        // Check if this chicken has a chaos trait
        if (!ChickenDataUtil.hasTrait(plugin, chicken)) return;

        TraitType traitType = ChickenDataUtil.getTrait(plugin, chicken);
        if (traitType == TraitType.EMPTY) return;

        // Get the BukkitTrait implementation
        BukkitTrait trait = plugin.getTraitMap().get(traitType);
        if (trait != null) {
            trait.onDeath(chicken, event);
        }

        // Grant advancement if killed by player
        org.bukkit.entity.Player killer = chicken.getKiller();
        if (killer != null) {
            org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(),
                    "advancement grant " + killer.getName() + " only chaoschickens:kill_" + traitType.getKey());
        }

        // Remove from active chickens map
        plugin.removeActiveChicken(chicken.getUniqueId());
    }
}
