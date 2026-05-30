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
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Boss chicken trait.
 * A powerful, large boss chicken with 80 HP, boss health bar, and multi-layered attacks:
 * - Launching fireballs at players (including creative mode).
 * - Throwing falling blocks.
 * - Summoning lightning strikes.
 * - Summoning zombie minion chickens.
 * - Dealing weakness potion effects to players nearby.
 * - Massive death explosion (8.0f power with block destruction and fire).
 * - High-tier loot drops (Nether Star and 2-4 Diamonds).
 */
public class BossTrait extends FabricTrait {

    private static final Random RANDOM = new Random();

    private static final java.util.Map<java.util.UUID, ServerBossBar> bossEvents = new java.util.concurrent.ConcurrentHashMap<>();

    /** Track the last teleport game time to prevent boss from teleporting too often. */
    private static final java.util.Map<java.util.UUID, Long> lastTeleportTime = new java.util.concurrent.ConcurrentHashMap<>();

    /** Track local tick counter per boss chicken to guarantee correct tick timing. */
    private static final java.util.Map<java.util.UUID, Integer> bossTicks = new java.util.concurrent.ConcurrentHashMap<>();

    /** Minimum ticks between boss teleports (10 seconds). */
    private static final long BOSS_TELEPORT_COOLDOWN = 200L;

    public static void clearBossBar(java.util.UUID uuid) {
        ServerBossBar event = bossEvents.remove(uuid);
        if (event != null) {
            event.clearPlayers();
        }
        lastTeleportTime.remove(uuid);
        bossTicks.remove(uuid);
    }

    public BossTrait() {
        super(TraitType.BOSS, "A powerful chicken with multiple chaos traits combined!", 0.02,
                true, true, 20); // Ticks every 20 ticks (1s)
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
        var maxHealthAttr = chicken.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(80.0);
            chicken.setHealth(80.0f);
        }
    }

    @Override
    public void onTick(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved() || chicken.isDead()) return;
        if (!(chicken.getWorld() instanceof ServerWorld serverWorld)) return;

        long gameTime = serverWorld.getTime();
        int tick = bossTicks.merge(chicken.getUuid(), 20, Integer::sum);

        // --- Boss Bar Management ---
        ServerBossBar bossEvent = bossEvents.computeIfAbsent(chicken.getUuid(), uuid -> {
            ServerBossBar event = new ServerBossBar(
                    Text.literal("BOSS Chicken").formatted(Formatting.DARK_RED, Formatting.BOLD),
                    BossBar.Color.RED,
                    BossBar.Style.PROGRESS
            );
            event.setVisible(true);
            return event;
        });

        bossEvent.setPercent(chicken.getHealth() / chicken.getMaxHealth());

        java.util.Set<ServerPlayerEntity> currentPlayers = new java.util.HashSet<>();
        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            if (player.squaredDistanceTo(chicken) <= 32.0 * 32.0) {
                currentPlayers.add(player);
            }
        }
        for (ServerPlayerEntity player : currentPlayers) {
            if (!bossEvent.getPlayers().contains(player)) {
                bossEvent.addPlayer(player);
            }
        }
        java.util.List<ServerPlayerEntity> toRemove = new java.util.ArrayList<>();
        for (ServerPlayerEntity player : bossEvent.getPlayers()) {
            if (!currentPlayers.contains(player)) {
                toRemove.add(player);
            }
        }
        for (ServerPlayerEntity player : toRemove) {
            bossEvent.removePlayer(player);
        }

        // --- Epic Animations & Particles ---
        if (ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            double time = tick * 0.15;
            double radius = 1.2;
            double offsetX = Math.cos(time) * radius;
            double offsetZ = Math.sin(time) * radius;
            serverWorld.spawnParticles(
                ParticleTypes.WITCH,
                chicken.getX() + offsetX, chicken.getY() + 0.8, chicken.getZ() + offsetZ,
                2, 0.0, 0.0, 0.0, 0.0
            );
            serverWorld.spawnParticles(
                ParticleTypes.PORTAL,
                chicken.getX() - offsetX, chicken.getY() + 0.8, chicken.getZ() - offsetZ,
                2, 0.0, 0.0, 0.0, 0.0
            );

            if (tick % 10 == 0) {
                serverWorld.spawnParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    chicken.getX(), chicken.getY() + 1.5, chicken.getZ(),
                    5, 0.2, 0.2, 0.2, 0.02
                );
            }

            // Aura effect
            if (tick % 5 == 0) {
                serverWorld.spawnParticles(
                    ParticleTypes.ENCHANT,
                    chicken.getX(), chicken.getY() + 0.3, chicken.getZ(),
                    3, 0.5, 0.1, 0.5, 0.01
                );
            }
        }

        // --- Boss Melodic hum ---
        if (tick % 40 == 0) {
            serverWorld.playSound(
                null,
                chicken.getX(), chicken.getY(), chicken.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_BASS,
                SoundCategory.HOSTILE,
                1.5f, 0.5f
            );
        }

        // --- Fireball Projectile Attacks (every 4 seconds) ---
        if (tick % 80 == 0) {
            PlayerEntity targetPlayer = findNearestSurvivalPlayer(serverWorld, chicken, 16.0);

            if (targetPlayer != null) {
                Vec3d shootVec = new Vec3d(
                        targetPlayer.getX() - chicken.getX(),
                        (targetPlayer.getY() + 0.8) - chicken.getBodyY(0.5),
                        targetPlayer.getZ() - chicken.getZ()
                );
                SmallFireballEntity fireball = new SmallFireballEntity(serverWorld, chicken, shootVec);
                fireball.setPosition(chicken.getX(), chicken.getBodyY(0.8), chicken.getZ());
                serverWorld.spawnEntity(fireball);

                serverWorld.playSound(
                        null,
                        chicken.getX(), chicken.getY(), chicken.getZ(),
                        SoundEvents.ENTITY_GHAST_SHOOT,
                        SoundCategory.HOSTILE,
                        1.5f, 1.0f
                );
            }
        }

        // --- Surrounding Block Control (Block Throwing every 3 seconds) ---
        if (tick % 60 == 0) {
            PlayerEntity targetPlayer = findNearestSurvivalPlayer(serverWorld, chicken, 12.0);

            if (targetPlayer != null) {
                BlockPos playerPos = targetPlayer.getBlockPos();
                BlockPos groundPos = null;
                for (int attempt = 0; attempt < 15; attempt++) {
                    int rx = RANDOM.nextInt(9) - 4;
                    int rz = RANDOM.nextInt(9) - 4;
                    BlockPos checkPos = playerPos.add(rx, -1, rz);
                    BlockState state = serverWorld.getBlockState(checkPos);
                    if (!state.isAir() && !state.isOf(Blocks.BEDROCK) && !state.isOf(Blocks.BARRIER)) {
                        groundPos = checkPos;
                        break;
                    }
                }

                if (groundPos != null) {
                    BlockState blockState = serverWorld.getBlockState(groundPos);
                    serverWorld.setBlockState(groundPos, Blocks.AIR.getDefaultState());

                    BlockPos spawnPos = groundPos.up();
                    FallingBlockEntity fallingBlock = FallingBlockEntity.spawnFromBlock(
                            serverWorld,
                            spawnPos,
                            blockState
                    );

                    if (fallingBlock != null) {
                        Vec3d targetVec = new Vec3d(
                                targetPlayer.getX() - spawnPos.getX(),
                                (targetPlayer.getY() + 1.2) - spawnPos.getY(),
                                targetPlayer.getZ() - spawnPos.getZ()
                        );
                        double distance = targetVec.length();
                        if (distance > 0) {
                            Vec3d velocity = targetVec.multiply(0.85 / distance);
                            fallingBlock.setVelocity(velocity);
                            fallingBlock.velocityModified = true;
                        }

                        serverWorld.playSound(
                                null,
                                spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(),
                                SoundEvents.ENTITY_WITHER_SHOOT,
                                SoundCategory.HOSTILE,
                                1.5f, 0.5f
                        );

                        if (ConfigLoader.getConfig().isTraitParticlesEnabled()) {
                            serverWorld.spawnParticles(
                                    ParticleTypes.EXPLOSION,
                                    spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5,
                                    5, 0.2, 0.2, 0.2, 0.05
                            );
                        }
                    }
                }
            }
        }

        // --- Lightning Strike Attack (every 6 seconds) ---
        if (tick % 120 == 0) {
            PlayerEntity targetPlayer = findNearestSurvivalPlayer(serverWorld, chicken, 16.0);
            if (targetPlayer != null) {
                LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, serverWorld);
                lightning.setPosition(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ());
                serverWorld.spawnEntity(lightning);
            }
        }

        // --- Summon Zombie Chicken Minions (every 8 seconds) ---
        if (tick % 160 == 0) {
            java.util.List<ChickenEntity> nearbyMinions = serverWorld.getEntitiesByClass(
                    ChickenEntity.class,
                    chicken.getBoundingBox().expand(16.0),
                    otherChicken -> ChaosChickensFabric.getActiveTrait(otherChicken).orElse(TraitType.EMPTY) == TraitType.ZOMBIE
            );
            if (nearbyMinions.size() < 6) {
                for (int i = 0; i < 2; i++) {
                    ChickenEntity minion = EntityType.CHICKEN.create(serverWorld);
                    if (minion != null) {
                        minion.refreshPositionAndAngles(
                            chicken.getX() + RANDOM.nextDouble() * 3.0 - 1.5,
                            chicken.getY(),
                            chicken.getZ() + RANDOM.nextDouble() * 3.0 - 1.5,
                            0.0f, 0.0f
                        );
                        ChaosChickensFabric.assignTrait(minion, TraitType.ZOMBIE);
                        serverWorld.spawnEntity(minion);
                    }
                }
                if (ConfigLoader.getConfig().isTraitParticlesEnabled()) {
                    serverWorld.spawnParticles(
                        ParticleTypes.WITCH,
                        chicken.getX(), chicken.getY() + 1.0, chicken.getZ(),
                        20, 1.0, 0.5, 1.0, 0.1
                    );
                }
                serverWorld.playSound(
                    null,
                    chicken.getX(), chicken.getY(), chicken.getZ(),
                    SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED,
                    SoundCategory.HOSTILE,
                    1.5f, 0.8f
                );
            }
        }

        // --- Tick sub-traits, but SKIP TeleportTrait (handled separately) ---
        java.util.List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(chicken);
        for (TraitType type : subTraits) {
            if (type == TraitType.TELEPORT) continue;

            com.chaoschickens.fabric.trait.FabricTrait trait = ChaosChickensFabric.getTraitInstance(type);
            if (trait != null && trait.isPeriodic()) {
                trait.onTick(chicken);
            }
        }

        // --- Boss-controlled teleport with strict cooldown (every 10 seconds max) ---
        if (subTraits.contains(TraitType.TELEPORT)) {
            long lastTp = lastTeleportTime.getOrDefault(chicken.getUuid(), 0L);
            if (gameTime - lastTp >= BOSS_TELEPORT_COOLDOWN) {
                if (tick % 200 == 0) {
                    com.chaoschickens.fabric.trait.FabricTrait tpTrait = ChaosChickensFabric.getTraitInstance(TraitType.TELEPORT);
                    if (tpTrait instanceof TeleportTrait teleportTrait) {
                        teleportTrait.teleport(chicken, serverWorld);
                        lastTeleportTime.put(chicken.getUuid(), gameTime);
                    }
                }
            }
        }
    }

    @Override
    public void onDeath(ChickenEntity chicken, DamageSource source) {
        if (chicken == null) return;
        clearBossBar(chicken.getUuid());

        if (!(chicken.getWorld() instanceof ServerWorld serverWorld)) return;

        // === MASSIVE DEATH EXPLOSION ===
        serverWorld.createExplosion(
                chicken,
                chicken.getX(), chicken.getY(), chicken.getZ(),
                8.0f, // power
                World.ExplosionSourceType.TNT
        );

        // Particle effects
        if (ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                chicken.getX(), chicken.getY(), chicken.getZ(),
                3, 0.0, 0.0, 0.0, 0.0);

            // Soul fire flames ring
            for (int i = 0; i < 36; i++) {
                double angle = i * Math.PI * 2.0 / 36.0;
                double px = chicken.getX() + Math.cos(angle) * 3.0;
                double pz = chicken.getZ() + Math.sin(angle) * 3.0;
                serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    px, chicken.getY() + 0.5, pz,
                    3, 0.1, 0.5, 0.1, 0.05);
            }

            // Enchant sparks
            serverWorld.spawnParticles(ParticleTypes.ENCHANT,
                chicken.getX(), chicken.getY() + 1.0, chicken.getZ(),
                30, 1.5, 1.5, 1.5, 0.1);

            // Witch sparks
            serverWorld.spawnParticles(ParticleTypes.WITCH,
                chicken.getX(), chicken.getY() + 1.0, chicken.getZ(),
                30, 1.5, 1.5, 1.5, 0.1);
        }

        // Sound effects
        serverWorld.playSound(null,
                chicken.getX(), chicken.getY(), chicken.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.HOSTILE,
                3.0f, 0.5f);

        serverWorld.playSound(null,
                chicken.getX(), chicken.getY(), chicken.getZ(),
                SoundEvents.ENTITY_WITHER_DEATH,
                SoundCategory.HOSTILE,
                2.0f, 1.0f);

        // Drop star & diamonds
        chicken.dropStack(new ItemStack(Items.NETHER_STAR, 1));
        chicken.dropStack(new ItemStack(Items.DIAMOND, 2 + RANDOM.nextInt(3)));

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

    @Override
    public void onDamage(ChickenEntity chicken, DamageSource source, float amount) {
        // Don't teleport on damage - boss stands its ground! Play an angry sound
        if (chicken != null && !chicken.isRemoved() && chicken.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null,
                    chicken.getX(), chicken.getY(), chicken.getZ(),
                    SoundEvents.ENTITY_ENDER_DRAGON_GROWL,
                    SoundCategory.HOSTILE,
                    0.5f, 1.5f);
        }
    }

    /** Find the nearest player within range (including creative players so testing is easy). */
    private static PlayerEntity findNearestSurvivalPlayer(ServerWorld world, ChickenEntity chicken, double range) {
        PlayerEntity target = null;
        double nearestDistSq = range * range;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!player.isSpectator() && player.isAlive()) {
                double distSq = player.squaredDistanceTo(chicken);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    target = player;
                }
            }
        }
        return target;
    }
}
