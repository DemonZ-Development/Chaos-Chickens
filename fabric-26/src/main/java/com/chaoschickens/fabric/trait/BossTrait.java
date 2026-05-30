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
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.Random;

public class BossTrait extends FabricTrait {

    private static final Random RANDOM = new Random();

    private static final java.util.Map<java.util.UUID, net.minecraft.server.level.ServerBossEvent> bossEvents = new java.util.concurrent.ConcurrentHashMap<>();

    /** Track the last teleport game time to prevent boss from teleporting too often. */
    private static final java.util.Map<java.util.UUID, Long> lastTeleportTime = new java.util.concurrent.ConcurrentHashMap<>();

    /** Track local tick counter per boss chicken to guarantee correct tick timing. */
    private static final java.util.Map<java.util.UUID, Integer> bossTicks = new java.util.concurrent.ConcurrentHashMap<>();

    /** Minimum ticks between boss teleports (10 seconds). */
    private static final long BOSS_TELEPORT_COOLDOWN = 200L;

    public static void clearBossBar(java.util.UUID uuid) {
        net.minecraft.server.level.ServerBossEvent event = bossEvents.remove(uuid);
        if (event != null) {
            event.removeAllPlayers();
        }
        lastTeleportTime.remove(uuid);
        bossTicks.remove(uuid);
    }

    public BossTrait() {
        super(TraitType.BOSS, "A powerful chicken with multiple chaos traits combined!", 0.02,
                true, true, 20);
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
            com.chaoschickens.fabric.trait.FabricTrait trait = ChaosChickensFabric.getTraitInstance(type);
            if (trait != null) {
                trait.onApply(chicken);
            }
        }

        // Set Boss name and health (ensure they take precedence over sub-traits)
        chicken.setCustomName(Component.literal("BOSS Chicken").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        chicken.setCustomNameVisible(true);
        if (chicken.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) != null) {
            chicken.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(80.0);
            chicken.setHealth(80.0f);
        }
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isRemoved() || chicken.isDeadOrDying()) return;
        if (!(chicken.level() instanceof ServerLevel serverWorld)) return;

        long gameTime = serverWorld.getGameTime();
        int tick = bossTicks.merge(chicken.getUUID(), 20, Integer::sum);

        // --- Boss Bar Management ---
        net.minecraft.server.level.ServerBossEvent bossEvent = bossEvents.computeIfAbsent(chicken.getUUID(), uuid -> {
            net.minecraft.server.level.ServerBossEvent event = new net.minecraft.server.level.ServerBossEvent(
                    uuid,
                    Component.literal("BOSS Chicken").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    net.minecraft.world.BossEvent.BossBarColor.RED,
                    net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS
            );
            event.setVisible(true);
            return event;
        });

        bossEvent.setProgress(chicken.getHealth() / chicken.getMaxHealth());

        java.util.Set<net.minecraft.server.level.ServerPlayer> currentPlayers = new java.util.HashSet<>();
        for (net.minecraft.server.level.ServerPlayer player : serverWorld.players()) {
            if (player.distanceToSqr(chicken) <= 32.0 * 32.0) {
                currentPlayers.add(player);
            }
        }
        for (net.minecraft.server.level.ServerPlayer player : currentPlayers) {
            if (!bossEvent.getPlayers().contains(player)) {
                bossEvent.addPlayer(player);
            }
        }
        java.util.List<net.minecraft.server.level.ServerPlayer> toRemove = new java.util.ArrayList<>();
        for (net.minecraft.server.level.ServerPlayer player : bossEvent.getPlayers()) {
            if (!currentPlayers.contains(player)) {
                toRemove.add(player);
            }
        }
        for (net.minecraft.server.level.ServerPlayer player : toRemove) {
            bossEvent.removePlayer(player);
        }

        // --- Epic Animations & Particles ---
        if (ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            double time = tick * 0.15;
            double radius = 1.2;
            double offsetX = Math.cos(time) * radius;
            double offsetZ = Math.sin(time) * radius;
            serverWorld.sendParticles(
                ParticleTypes.WITCH,
                chicken.getX() + offsetX, chicken.getY() + 0.8, chicken.getZ() + offsetZ,
                2, 0.0, 0.0, 0.0, 0.0
            );
            serverWorld.sendParticles(
                ParticleTypes.PORTAL,
                chicken.getX() - offsetX, chicken.getY() + 0.8, chicken.getZ() - offsetZ,
                2, 0.0, 0.0, 0.0, 0.0
            );

            if (tick % 10 == 0) {
                serverWorld.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    chicken.getX(), chicken.getY() + 1.5, chicken.getZ(),
                    5, 0.2, 0.2, 0.2, 0.02
                );
            }

            // Aura effect
            if (tick % 5 == 0) {
                serverWorld.sendParticles(
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
                chicken,
                net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value(),
                net.minecraft.sounds.SoundSource.HOSTILE,
                1.5f, 0.5f
            );
        }

        // --- Fireball Projectile Attacks (every 4 seconds) ---
        if (tick % 80 == 0) {
            Player targetPlayer = findNearestSurvivalPlayer(serverWorld, chicken, 16.0);

            if (targetPlayer != null) {
                net.minecraft.world.phys.Vec3 shootVec = new net.minecraft.world.phys.Vec3(
                        targetPlayer.getX() - chicken.getX(),
                        (targetPlayer.getY() + 0.8) - chicken.getY(0.5),
                        targetPlayer.getZ() - chicken.getZ()
                );
                net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball fireball = new net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball(
                        serverWorld,
                        chicken,
                        shootVec
                );
                fireball.setPos(chicken.getX(), chicken.getY(0.8), chicken.getZ());
                serverWorld.addFreshEntity(fireball);

                serverWorld.playSound(
                        null,
                        chicken.getX(), chicken.getY(), chicken.getZ(),
                        net.minecraft.sounds.SoundEvents.GHAST_SHOOT,
                        net.minecraft.sounds.SoundSource.HOSTILE,
                        1.5f, 1.0f
                );
            }
        }

        // --- Surrounding Block Control (Block Throwing every 3 seconds) ---
        if (tick % 60 == 0) {
            Player targetPlayer = findNearestSurvivalPlayer(serverWorld, chicken, 12.0);

            if (targetPlayer != null) {
                var random = chicken.getRandom();
                net.minecraft.core.BlockPos playerPos = targetPlayer.blockPosition();
                net.minecraft.core.BlockPos groundPos = null;
                for (int attempt = 0; attempt < 15; attempt++) {
                    int rx = random.nextInt(9) - 4;
                    int rz = random.nextInt(9) - 4;
                    net.minecraft.core.BlockPos checkPos = playerPos.offset(rx, -1, rz);
                    net.minecraft.world.level.block.state.BlockState state = serverWorld.getBlockState(checkPos);
                    if (!state.isAir() && state.isSolid() && !state.is(net.minecraft.world.level.block.Blocks.BEDROCK)
                            && !state.is(net.minecraft.world.level.block.Blocks.BARRIER)) {
                        groundPos = checkPos;
                        break;
                    }
                }

                if (groundPos != null) {
                    net.minecraft.world.level.block.state.BlockState blockState = serverWorld.getBlockState(groundPos);
                    serverWorld.setBlockAndUpdate(groundPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());

                    net.minecraft.core.BlockPos spawnPos = groundPos.above();
                    net.minecraft.world.entity.item.FallingBlockEntity fallingBlock = net.minecraft.world.entity.item.FallingBlockEntity.fall(
                            serverWorld,
                            spawnPos,
                            blockState
                    );

                    if (fallingBlock != null) {
                        net.minecraft.world.phys.Vec3 targetVec = new net.minecraft.world.phys.Vec3(
                                targetPlayer.getX() - spawnPos.getX(),
                                (targetPlayer.getY() + 1.2) - spawnPos.getY(),
                                targetPlayer.getZ() - spawnPos.getZ()
                        );
                        double distance = targetVec.length();
                        if (distance > 0) {
                            net.minecraft.world.phys.Vec3 velocity = targetVec.scale(0.85 / distance);
                            fallingBlock.setDeltaMovement(velocity);
                        }

                        serverWorld.playSound(
                                null,
                                spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(),
                                net.minecraft.sounds.SoundEvents.WITHER_SHOOT,
                                net.minecraft.sounds.SoundSource.HOSTILE,
                                1.5f, 0.5f
                        );

                        serverWorld.sendParticles(
                                ParticleTypes.EXPLOSION,
                                spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5,
                                5, 0.2, 0.2, 0.2, 0.05
                        );
                    }
                }
            }
        }

        // --- Lightning Strike Attack (every 6 seconds) ---
        if (tick % 120 == 0) {
            Player targetPlayer = findNearestSurvivalPlayer(serverWorld, chicken, 16.0);
            if (targetPlayer != null) {
                net.minecraft.world.entity.LightningBolt lightning = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(
                        serverWorld, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
                if (lightning != null) {
                    lightning.setPos(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ());
                    serverWorld.addFreshEntity(lightning);
                }
            }
        }

        // --- Summon Zombie Chicken Minions (every 8 seconds) ---
        if (tick % 160 == 0) {
            java.util.List<Chicken> nearbyMinions = serverWorld.getEntitiesOfClass(
                    Chicken.class,
                    chicken.getBoundingBox().inflate(16.0),
                    otherChicken -> ChaosChickensFabric.getActiveTrait(otherChicken).orElse(TraitType.EMPTY) == TraitType.ZOMBIE
            );
            if (nearbyMinions.size() < 6) {
                // Summon 2 Zombie Chickens to defend the boss
                for (int i = 0; i < 2; i++) {
                    Chicken minion = net.minecraft.world.entity.EntityType.CHICKEN.create(
                            serverWorld, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
                    if (minion != null) {
                        minion.setPos(
                            chicken.getX() + RANDOM.nextDouble() * 3.0 - 1.5,
                            chicken.getY(),
                            chicken.getZ() + RANDOM.nextDouble() * 3.0 - 1.5
                        );
                        ChaosChickensFabric.assignTrait(minion, TraitType.ZOMBIE);
                        serverWorld.addFreshEntity(minion);
                    }
                }
                // Witch particles to denote summoning
                if (ConfigLoader.getConfig().isTraitParticlesEnabled()) {
                    serverWorld.sendParticles(
                        ParticleTypes.WITCH,
                        chicken.getX(), chicken.getY() + 1.0, chicken.getZ(),
                        20, 1.0, 0.5, 1.0, 0.1
                    );
                }
                serverWorld.playSound(
                    null,
                    chicken.getX(), chicken.getY(), chicken.getZ(),
                    net.minecraft.sounds.SoundEvents.ZOMBIE_VILLAGER_CONVERTED,
                    net.minecraft.sounds.SoundSource.HOSTILE,
                    1.5f, 0.8f
                );
            }
        }

        // --- Tick sub-traits, but SKIP TeleportTrait (handle it with cooldown) ---
        java.util.List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(chicken);
        for (TraitType type : subTraits) {
            // Skip teleport - boss handles its own teleporting with cooldown
            if (type == TraitType.TELEPORT) continue;

            com.chaoschickens.fabric.trait.FabricTrait trait = ChaosChickensFabric.getTraitInstance(type);
            if (trait != null && trait.isPeriodic()) {
                trait.onTick(chicken);
            }
        }

        // --- Boss-controlled teleport with strict cooldown (every 10 seconds max) ---
        if (subTraits.contains(TraitType.TELEPORT)) {
            long lastTp = lastTeleportTime.getOrDefault(chicken.getUUID(), 0L);
            if (gameTime - lastTp >= BOSS_TELEPORT_COOLDOWN) {
                if (tick % 200 == 0) { // Only teleport periodically
                    com.chaoschickens.fabric.trait.FabricTrait tpTrait = ChaosChickensFabric.getTraitInstance(TraitType.TELEPORT);
                    if (tpTrait instanceof TeleportTrait teleportTrait) {
                        teleportTrait.teleport(chicken, serverWorld);
                        lastTeleportTime.put(chicken.getUUID(), gameTime);
                    }
                }
            }
        }
    }

    @Override
    public void onDeath(Chicken chicken, net.minecraft.world.damagesource.DamageSource source) {
        if (chicken == null) return;
        clearBossBar(chicken.getUUID());

        if (!(chicken.level() instanceof ServerLevel serverWorld)) return;

        // === MASSIVE DEATH EXPLOSION ===
        // Create a real explosion that damages blocks and entities, but doesn't hurt the boss itself
        serverWorld.explode(
                chicken, // source entity
                chicken.getX(), chicken.getY(), chicken.getZ(),
                8.0f, // power (TNT = 4, Creeper = 3, Charged Creeper = 6, Ender Dragon = 8)
                true, // create fire!
                net.minecraft.world.level.Level.ExplosionInteraction.TNT // destroy blocks like TNT
        );

        // Massive particle effects
        if (ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            // Big explosion
            serverWorld.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                chicken.getX(), chicken.getY(), chicken.getZ(),
                3, 0.0, 0.0, 0.0, 0.0);

            // Soul fire flames ring
            for (int i = 0; i < 36; i++) {
                double angle = i * Math.PI * 2.0 / 36.0;
                double px = chicken.getX() + Math.cos(angle) * 3.0;
                double pz = chicken.getZ() + Math.sin(angle) * 3.0;
                serverWorld.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    px, chicken.getY() + 0.5, pz,
                    3, 0.1, 0.5, 0.1, 0.05);
            }

            // Witch sparks
            serverWorld.sendParticles(ParticleTypes.ENCHANT,
                chicken.getX(), chicken.getY() + 1.0, chicken.getZ(),
                30, 1.5, 1.5, 1.5, 0.1);

            // Witch sparks
            serverWorld.sendParticles(ParticleTypes.WITCH,
                chicken.getX(), chicken.getY() + 1.0, chicken.getZ(),
                30, 1.5, 1.5, 1.5, 0.1);
        }

        // Dramatic sound effects
        serverWorld.playSound(null,
                chicken.getX(), chicken.getY(), chicken.getZ(),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                net.minecraft.sounds.SoundSource.HOSTILE,
                3.0f, 0.5f);

        serverWorld.playSound(null,
                chicken.getX(), chicken.getY(), chicken.getZ(),
                net.minecraft.sounds.SoundEvents.WITHER_DEATH,
                net.minecraft.sounds.SoundSource.HOSTILE,
                2.0f, 1.0f);

        // Drop special loot
        chicken.spawnAtLocation(serverWorld, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHER_STAR, 1));
        chicken.spawnAtLocation(serverWorld, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 2 + RANDOM.nextInt(3)));

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
    public void onPlayerNear(Chicken chicken, Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));

        // Delegate proximity effects for all sub-traits!
        java.util.List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(chicken);
        for (TraitType type : subTraits) {
            com.chaoschickens.fabric.trait.FabricTrait trait = ChaosChickensFabric.getTraitInstance(type);
            if (trait != null && trait.getProximityRange() > 0) {
                double distSq = chicken.distanceToSqr(player);
                if (distSq <= trait.getProximityRange() * trait.getProximityRange()) {
                    trait.onPlayerNear(chicken, player);
                }
            }
        }
    }

    @Override
    public void onDamage(Chicken chicken, net.minecraft.world.damagesource.DamageSource source, float amount) {
        // Don't teleport on damage - boss stands its ground!
        // Just play an angry sound
        if (chicken != null && !chicken.isRemoved() && chicken.level() instanceof ServerLevel serverWorld) {
            serverWorld.playSound(null,
                    chicken.getX(), chicken.getY(), chicken.getZ(),
                    net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
                    net.minecraft.sounds.SoundSource.HOSTILE,
                    0.5f, 1.5f);
        }
    }

    /** Find the nearest player within range (including creative players so testing is extremely cool). */
    private static Player findNearestSurvivalPlayer(ServerLevel world, Chicken chicken, double range) {
        Player target = null;
        double nearestDistSq = range * range;
        for (Player player : world.players()) {
            if (!player.isSpectator() && !player.isDeadOrDying()) {
                double distSq = player.distanceToSqr(chicken);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    target = player;
                }
            }
        }
        return target;
    }
}
