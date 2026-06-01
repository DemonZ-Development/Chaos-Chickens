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

        // Grant advancement if killed by player or recently hurt by player
        org.bukkit.entity.Player killer = chicken.getKiller();
        if (killer == null) {
            killer = plugin.getLastAttacker(chicken.getUniqueId());
            if (killer != null) {
                plugin.getLogger().info("No direct killer found, using last attacker: " + killer.getName());
            }
        } else {
            plugin.getLogger().info("Direct killer found: " + killer.getName());
        }

        // Fallback to nearest player within 32 blocks for BOSS deaths (helps with command kills/explosions)
        if (killer == null && traitType == TraitType.BOSS) {
            double nearestDistSq = 32.0 * 32.0;
            for (org.bukkit.entity.Player p : chicken.getWorld().getPlayers()) {
                if (p.isValid() && !p.isDead() && p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                    double distSq = p.getLocation().distanceSquared(chicken.getLocation());
                    if (distSq < nearestDistSq) {
                        killer = p;
                        nearestDistSq = distSq;
                    }
                }
            }
            if (killer != null) {
                plugin.getLogger().info("No killer/attacker found for BOSS. Falling back to nearest player: " + killer.getName());
            }
        }

        if (killer != null) {
            String cmd = "advancement grant " + killer.getName() + " only chaoschickens:kill_" + traitType.getKey();
            plugin.getLogger().info("Attempting to execute command: " + cmd);
            try {
                boolean success = org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), cmd);
                plugin.getLogger().info("Advancement command executed with result: " + success);
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to run advancement command: " + cmd, e);
            }
        } else {
            plugin.getLogger().warning("Could not grant advancement for " + traitType.getKey() + " chicken because no killer/attacker/nearest player was found.");
        }

        // Remove from active chickens map
        plugin.removeActiveChicken(chicken.getUniqueId());
    }
}
