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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BOSS chicken trait — the most powerful and dangerous chicken.
 * Combines multiple sub-traits, attacks with fireballs,
 * teleports on cooldown, has massive HP, and creates a huge explosion on death.
 */
public class BossTrait extends BukkitTrait {

    /** Track last teleport time per boss to prevent teleport spam (10s cooldown). */
    private static final Map<UUID, Long> lastTeleportTime = new ConcurrentHashMap<>();

    /** Track local tick counter per boss chicken to guarantee correct tick timing. */
    private static final Map<UUID, Integer> bossTicks = new ConcurrentHashMap<>();

    private static final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();

    public BossTrait() {
        super(TraitType.BOSS, "A powerful chicken with multiple chaos traits combined!", 0.02,
                true, true, 10); // Ticked frequently for sub-traits
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;

        // Load or roll sub-traits
        List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(plugin, chicken);
        if (subTraits.isEmpty()) {
            int count = plugin.getConfigManager().getBossTraitCount();
            List<TraitType> available = new ArrayList<>();
            for (TraitType t : TraitType.values()) {
                if (t != TraitType.EMPTY && t != TraitType.BOSS && t != TraitType.CUSTOM) {
                    if (com.chaoschickens.api.ChaosChickensAPI.isTraitEnabled(t) &&
                        com.chaoschickens.api.ChaosChickensAPI.getTraitWeight(t) > 0.0) {
                        available.add(t);
                    }
                }
            }
            Collections.shuffle(available);
            for (int i = 0; i < Math.min(count, available.size()); i++) {
                subTraits.add(available.get(i));
            }
            ChickenDataUtil.setBossSubTraits(plugin, chicken, subTraits);
        }

        // Apply all sub-traits
        for (TraitType type : subTraits) {
            BukkitTrait trait = plugin.getTraitMap().get(type);
            if (trait != null) {
                trait.onApply(chicken);
            }
        }

        // Boss name and massive health (override sub-trait names)
        chicken.setCustomName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "☠ BOSS Chicken ☠");
        chicken.setCustomNameVisible(true);
        chicken.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(80.0);
        chicken.setHealth(80.0);
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;

        UUID uuid = chicken.getUniqueId();
        int tick = bossTicks.merge(uuid, 10, Integer::sum);

        // --- Boss Bar Management ---
        BossBar bossBar = bossBars.computeIfAbsent(uuid, id -> {
            BossBar bar = org.bukkit.Bukkit.createBossBar(
                    ChatColor.DARK_RED + "" + ChatColor.BOLD + "☠ BOSS Chicken ☠",
                    BarColor.RED,
                    BarStyle.SOLID
            );
            bar.setVisible(true);
            return bar;
        });

        double maxHealth = chicken.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, chicken.getHealth() / maxHealth)));

        Set<Player> currentPlayers = new HashSet<>();
        double rangeSq = 32.0 * 32.0;
        for (Player player : chicken.getWorld().getPlayers()) {
            if (!player.isDead() && player.isValid() && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                if (player.getLocation().distanceSquared(chicken.getLocation()) <= rangeSq) {
                    currentPlayers.add(player);
                }
            }
        }

        for (Player player : currentPlayers) {
            if (!bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
        }

        List<Player> toRemove = new ArrayList<>();
        for (Player player : bossBar.getPlayers()) {
            if (!currentPlayers.contains(player)) {
                toRemove.add(player);
            }
        }
        for (Player player : toRemove) {
            bossBar.removePlayer(player);
        }

        // === Boss aura particles (rotating trail) ===
        if (plugin.getConfigManager().isTraitParticlesEnabled()) {
            double time = tick * 0.15;
            double radius = 1.2;
            double offsetX = Math.cos(time) * radius;
            double offsetZ = Math.sin(time) * radius;
            Location locWitch = chicken.getLocation().clone().add(offsetX, 0.8, offsetZ);
            Location locPortal = chicken.getLocation().clone().add(-offsetX, 0.8, -offsetZ);

            chicken.getWorld().spawnParticle(Particle.SPELL_WITCH,
                    locWitch, 2, 0.0, 0.0, 0.0, 0.0);
            chicken.getWorld().spawnParticle(Particle.PORTAL,
                    locPortal, 2, 0.0, 0.0, 0.0, 0.0);

            if (tick % 10 == 0) {
                chicken.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                        chicken.getLocation().clone().add(0, 1.5, 0),
                        5, 0.2, 0.2, 0.2, 0.02);
            }

            if (tick % 5 == 0) {
                chicken.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE,
                        chicken.getLocation().clone().add(0, 0.3, 0),
                        3, 0.5, 0.1, 0.5, 0.01);
            }
        }

        // === Boss teleport (10 second cooldown) ===
        long now = System.currentTimeMillis();
        long lastTp = lastTeleportTime.getOrDefault(uuid, 0L);
        if (now - lastTp > 10000) { // 10 seconds
            BukkitTrait teleportTrait = plugin.getTraitMap().get(TraitType.TELEPORT);
            if (teleportTrait instanceof TeleportTrait tp) {
                tp.teleport(chicken);
                lastTeleportTime.put(uuid, now);
            }
        }

        // === Fireball Projectile Attacks (every 80 ticks) ===
        if (tick % 80 == 0) {
            Player target = findNearestTarget(chicken, 16.0);
            if (target != null) {
                org.bukkit.util.Vector direction = target.getEyeLocation().toVector().subtract(chicken.getEyeLocation().toVector()).normalize();
                org.bukkit.entity.SmallFireball fireball = chicken.launchProjectile(org.bukkit.entity.SmallFireball.class, direction);
                fireball.setShooter(chicken);
                chicken.getWorld().playSound(chicken.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1.5f, 1.0f);
            }
        }

        // === Block Throwing Attack (every 60 ticks) ===
        if (tick % 60 == 0) {
            Player target = findNearestTarget(chicken, 12.0);
            if (target != null) {
                Location targetLoc = target.getLocation();
                org.bukkit.block.Block blockToLift = null;
                java.util.Random random = plugin.getRandom();
                // Find nearby solid block under player to lift
                for (int attempt = 0; attempt < 15; attempt++) {
                    int rx = random.nextInt(9) - 4;
                    int rz = random.nextInt(9) - 4;
                    org.bukkit.block.Block check = targetLoc.clone().add(rx, -1, rz).getBlock();
                    if (check.getType().isSolid() && check.getType() != Material.AIR 
                            && check.getType() != Material.BEDROCK && check.getType() != Material.BARRIER) {
                        blockToLift = check;
                        break;
                    }
                }

                if (blockToLift != null) {
                    org.bukkit.block.data.BlockData blockData = blockToLift.getBlockData();
                    blockToLift.setType(Material.AIR);

                    Location spawnLoc = blockToLift.getLocation().add(0.5, 1.0, 0.5);
                    org.bukkit.entity.FallingBlock fallingBlock = chicken.getWorld().spawnFallingBlock(spawnLoc, blockData);
                    fallingBlock.setDropItem(false);
                    fallingBlock.setHurtEntities(true);

                    org.bukkit.util.Vector targetVec = target.getEyeLocation().toVector().subtract(spawnLoc.toVector());
                    double distance = targetVec.length();
                    if (distance > 0) {
                        fallingBlock.setVelocity(targetVec.multiply(0.85 / distance));
                    }

                    chicken.getWorld().playSound(spawnLoc, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.5f);
                    chicken.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, spawnLoc, 5, 0.2, 0.2, 0.2, 0.05);
                }
            }
        }

        // === Boss melodic hum every 40 ticks ===
        if (tick % 40 == 0) {
            chicken.getWorld().playSound(chicken.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_BASS, 1.5f, 0.5f);
        }

        // === Lightning Strike Attack (every 120 ticks) ===
        if (tick % 120 == 0) {
            Player target = findNearestTarget(chicken, 16.0);
            if (target != null) {
                chicken.getWorld().strikeLightning(target.getLocation());
            }
        }

        // === Summon Zombie Chicken Minions (every 160 ticks) ===
        if (tick % 160 == 0) {
            int zombieCount = 0;
            for (org.bukkit.entity.Entity entity : chicken.getNearbyEntities(16.0, 16.0, 16.0)) {
                if (entity instanceof Chicken otherChicken) {
                    if (ChickenDataUtil.getTrait(plugin, otherChicken) == TraitType.ZOMBIE) {
                        zombieCount++;
                    }
                }
            }
            if (zombieCount < 6) {
                for (int i = 0; i < 2; i++) {
                    Location minionLoc = chicken.getLocation().clone().add(
                        plugin.getRandom().nextDouble() * 3.0 - 1.5,
                        0,
                        plugin.getRandom().nextDouble() * 3.0 - 1.5
                    );
                    Chicken minion = chicken.getWorld().spawn(minionLoc, Chicken.class);
                    plugin.assignTrait(minion, TraitType.ZOMBIE);
                }
                if (plugin.getConfigManager().isTraitParticlesEnabled()) {
                    chicken.getWorld().spawnParticle(Particle.SPELL_WITCH,
                            chicken.getLocation().clone().add(0, 1.0, 0),
                            20, 1.0, 0.5, 1.0, 0.1);
                }
                chicken.getWorld().playSound(chicken.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.5f, 0.8f);
            }
        }

        // === Tick sub-traits (but SKIP teleport — boss handles teleport itself) ===
        List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(plugin, chicken);
        for (TraitType type : subTraits) {
            if (type == TraitType.TELEPORT) continue; // Boss handles its own teleport
            BukkitTrait trait = plugin.getTraitMap().get(type);
            if (trait != null && trait.isPeriodic()) {
                trait.onTick(chicken);
            }
        }
    }

    private Player findNearestTarget(Chicken chicken, double range) {
        Player nearest = null;
        double minDistSq = range * range;
        for (Player p : chicken.getWorld().getPlayers()) {
            if (!p.isDead() && p.isValid() && p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                double distSq = chicken.getLocation().distanceSquared(p.getLocation());
                if (distSq < minDistSq) {
                    minDistSq = distSq;
                    nearest = p;
                }
            }
        }
        return nearest;
    }

    @Override
    public void onDeath(Chicken chicken, EntityDeathEvent event) {
        if (chicken == null) return;

        UUID uuid = chicken.getUniqueId();
        BossBar bar = bossBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }

        Location loc = chicken.getLocation();

        // === MASSIVE DEATH EXPLOSION ===
        chicken.getWorld().createExplosion(loc, 8.0f, true, true);

        // === Epic death particles ===
        if (plugin.getConfigManager().isTraitParticlesEnabled()) {
            // Explosion emitter
            try {
                chicken.getWorld().spawnParticle(Particle.valueOf("EXPLOSION_EMITTER"),
                        loc, 3, 0.5, 0.5, 0.5, 0.0);
            } catch (IllegalArgumentException e) {
                try {
                    chicken.getWorld().spawnParticle(Particle.valueOf("EXPLOSION_HUGE"),
                            loc, 3, 0.5, 0.5, 0.5, 0.0);
                } catch (IllegalArgumentException ignored) {}
            }

            // Soul fire ring (36 steps to match Fabric)
            for (int i = 0; i < 36; i++) {
                double angle = (i / 36.0) * Math.PI * 2;
                double rx = loc.getX() + Math.cos(angle) * 3.0;
                double rz = loc.getZ() + Math.sin(angle) * 3.0;
                chicken.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                        new Location(chicken.getWorld(), rx, loc.getY() + 0.5, rz),
                        3, 0.1, 0.5, 0.1, 0.05);
            }

            // Witch sparks
            chicken.getWorld().spawnParticle(Particle.SPELL_WITCH,
                    loc.clone().add(0, 1.0, 0), 30, 1.5, 1.5, 1.5, 0.1);

            // Enchant swirl
            chicken.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE,
                    loc.clone().add(0, 1.0, 0), 30, 1.5, 1.5, 1.5, 0.1);
        }

        // === Epic death sounds ===
        chicken.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);
        chicken.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 2.0f, 1.0f);

        // === Loot drops ===
        event.getDrops().add(new ItemStack(Material.NETHER_STAR, 1));
        int diamonds = 2 + plugin.getRandom().nextInt(3);
        event.getDrops().add(new ItemStack(Material.DIAMOND, diamonds));

        // === Trigger death for all sub-traits ===
        List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(plugin, chicken);
        for (TraitType type : subTraits) {
            BukkitTrait trait = plugin.getTraitMap().get(type);
            if (trait != null) {
                trait.onDeath(chicken, event);
            }
        }

        // Cleanup
        lastTeleportTime.remove(uuid);
        bossTicks.remove(uuid);
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

        // Delegate proximity effects for all sub-traits
        List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(plugin, chicken);
        for (TraitType type : subTraits) {
            BukkitTrait trait = plugin.getTraitMap().get(type);
            if (trait != null && trait.getProximityRange() > 0) {
                double dist = chicken.getLocation().distance(player.getLocation());
                if (dist <= trait.getProximityRange()) {
                    trait.onPlayerNear(chicken, player);
                }
            }
        }
    }

    /** Cleanup static map for removed bosses. */
    public static void cleanupBoss(UUID uuid) {
        lastTeleportTime.remove(uuid);
        bossTicks.remove(uuid);
        BossBar bar = bossBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
    }

    @Override
    public void onDamage(Chicken chicken, org.bukkit.event.entity.EntityDamageEvent event, double amount) {
        if (chicken == null || chicken.isDead()) return;
        chicken.getWorld().playSound(
                chicken.getLocation(),
                Sound.ENTITY_ENDER_DRAGON_GROWL,
                0.5f,
                1.5f
        );
    }
}
