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
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Listener that handles chicken spawn events.
 * When a chicken spawns, rolls for chaos chance and assigns a random trait.
 */
public class ChickenSpawnListener implements Listener {

    private final ChaosChickensBukkit plugin;
    /** Tracks pending egg traits: player UUID -> trait type. Set when player uses a chaos egg. */
    private final Map<UUID, TraitType> pendingEggTraits = new ConcurrentHashMap<>();

    public ChickenSpawnListener(ChaosChickensBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Replace monster spawns at night with zombie chickens (10% chance)
        if (!(event.getEntity() instanceof Chicken) && event.getEntity() instanceof org.bukkit.entity.Monster && event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            org.bukkit.World world = event.getEntity().getWorld();
            long time = world.getTime();
            boolean isNight = (time >= 13000 && time <= 23000);
            if (isNight && plugin.getRandom().nextDouble() < 0.10) {
                event.setCancelled(true);
                Chicken chicken = world.spawn(event.getEntity().getLocation(), Chicken.class);
                plugin.assignTrait(chicken, TraitType.ZOMBIE);
                return;
            }
        }

        // Only handle chicken spawns
        if (!(event.getEntity() instanceof Chicken)) return;

        Chicken chicken = (Chicken) event.getEntity();

        // Baby chickens always spawn with traits
        if (!chicken.isAdult()) {
            TraitType traitType = plugin.pickRandomTrait();
            if (traitType != TraitType.EMPTY) {
                plugin.assignTrait(chicken, traitType);
            }
            return;
        }

        // Natural spawns at night are always Zombie Chickens
        boolean isNight = (chicken.getWorld().getTime() >= 13000 && chicken.getWorld().getTime() <= 23000);
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL && isNight) {
            plugin.assignTrait(chicken, TraitType.ZOMBIE);
            return;
        }

        // Check if only natural spawns should be affected (allow spawn eggs to bypass)
        if (plugin.getConfigManager().isOnlyNaturalSpawns()) {
            CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
            if (reason != CreatureSpawnEvent.SpawnReason.NATURAL &&
                    reason != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
                return;
            }
        }

        // Handle spawn egg with chaos trait PDC
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            // Find the nearest player and check for a pending egg trait
            Player nearestPlayer = null;
            double nearestDist = Double.MAX_VALUE;
            for (org.bukkit.entity.Entity nearby : chicken.getNearbyEntities(10, 10, 10)) {
                if (nearby instanceof Player) {
                    double dist = nearby.getLocation().distanceSquared(chicken.getLocation());
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearestPlayer = (Player) nearby;
                    }
                }
            }
            if (nearestPlayer != null) {
                TraitType pending = pendingEggTraits.remove(nearestPlayer.getUniqueId());
                if (pending != null && pending != TraitType.EMPTY) {
                    plugin.assignTrait(chicken, pending);
                    return;
                }
            }
            // If only-natural-spawns is enabled, and this wasn't a custom chaos egg, do not roll a trait
            if (plugin.getConfigManager().isOnlyNaturalSpawns()) {
                return;
            }
        }

        // Check for max chickens per player limit
        int max = plugin.getConfigManager().getMaxChickensPerPlayer();
        if (max > -1) {
            Player nearestPlayer = null;
            double nearestDist = Double.MAX_VALUE;
            for (org.bukkit.entity.Entity nearby : chicken.getNearbyEntities(64, 64, 64)) {
                if (nearby instanceof Player) {
                    double dist = nearby.getLocation().distanceSquared(chicken.getLocation());
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearestPlayer = (Player) nearby;
                    }
                }
            }
            if (nearestPlayer != null) {
                int count = plugin.getActiveChickenCountForPlayer(nearestPlayer);
                if (count >= max) {
                    return;
                }
            }
        }

        // Check for boss chicken first (independent roll)
        if (plugin.getConfigManager().isBossChickensEnabled()) {
            double bossChance = plugin.getConfigManager().getBossChance();
            if (plugin.getRandom().nextDouble() < bossChance) {
                applyBossTrait(chicken);
                return;
            }
        }

        // Roll for chaos chance
        double chaosChance = plugin.getConfigManager().getChaosChance();
        if (!plugin.getConfigManager().isForceAllChickensToHaveTraits() && plugin.getRandom().nextDouble() >= chaosChance) return;

        // Pick a random trait and apply it
        TraitType traitType = plugin.pickRandomTrait();
        if (traitType == TraitType.EMPTY) return;

        plugin.assignTrait(chicken, traitType);
    }

    /**
     * Detect when a player uses a chaos chicken spawn egg and register the pending trait.
     * This ensures the spawned chicken gets the correct trait from the egg's PDC data.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.CHICKEN_SPAWN_EGG) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey eggTraitKey = new NamespacedKey(plugin, "egg_trait");
        if (!meta.getPersistentDataContainer().has(eggTraitKey, PersistentDataType.STRING)) return;

        String traitKey = meta.getPersistentDataContainer().get(eggTraitKey, PersistentDataType.STRING);
        if (traitKey != null) {
            TraitType traitType = TraitType.fromKey(traitKey);
            if (traitType != TraitType.EMPTY) {
                pendingEggTraits.put(event.getPlayer().getUniqueId(), traitType);
                if (plugin.isFolia()) {
                    try {
                        Class<?> globalSchedulerClass = Class.forName(
                                "io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
                        Object globalScheduler = org.bukkit.Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                        java.lang.reflect.Method runDelayed = globalSchedulerClass.getMethod("runDelayed",
                                org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, long.class);
                        runDelayed.invoke(globalScheduler, plugin, (java.util.function.Consumer<Object>) scheduledTask -> 
                                pendingEggTraits.remove(event.getPlayer().getUniqueId()), 100L);
                    } catch (Exception e) {
                        plugin.getServer().getScheduler().runTaskLater(plugin,
                                () -> pendingEggTraits.remove(event.getPlayer().getUniqueId()), 100L);
                    }
                } else {
                    plugin.getServer().getScheduler().runTaskLater(plugin,
                            () -> pendingEggTraits.remove(event.getPlayer().getUniqueId()), 100L);
                }
            }
        }
    }

    /**
     * Apply boss traits to a chicken (multiple traits combined).
     */
    private void applyBossTrait(Chicken chicken) {
        plugin.assignTrait(chicken, TraitType.BOSS);
        // onApply is already called inside assignTrait(), no duplicate call needed
    }

    @EventHandler
    public void onEntitiesLoad(org.bukkit.event.world.EntitiesLoadEvent event) {
        for (org.bukkit.entity.Entity entity : event.getEntities()) {
            if (entity instanceof Chicken chicken) {
                if (ChickenDataUtil.hasTrait(plugin, chicken)) {
                    TraitType trait = ChickenDataUtil.getTrait(plugin, chicken);
                    if (trait != TraitType.EMPTY) {
                        plugin.registerLoadedChicken(chicken, trait);
                    }
                } else if (plugin.getConfigManager().isForceAllChickensToHaveTraits()) {
                    // Make sure it hasn't been loaded/assigned already
                    if (!plugin.getChickenTrait(chicken).equals(TraitType.EMPTY)) continue;
                    
                    TraitType traitType = plugin.pickRandomTrait();
                    if (traitType != TraitType.EMPTY) {
                        plugin.assignTrait(chicken, traitType);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntitiesUnload(org.bukkit.event.world.EntitiesUnloadEvent event) {
        for (org.bukkit.entity.Entity entity : event.getEntities()) {
            if (entity instanceof Chicken) {
                plugin.unregisterLoadedChicken(entity.getUniqueId());
            }
        }
    }
}
