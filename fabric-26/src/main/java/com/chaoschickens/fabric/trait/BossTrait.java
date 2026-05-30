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

    public static void clearBossBar(java.util.UUID uuid) {
        net.minecraft.server.level.ServerBossEvent event = bossEvents.remove(uuid);
        if (event != null) {
            event.removeAllPlayers();
        }
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
            if (player.distanceToSqr(chicken) <= 16.0 * 16.0) {
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
            double time = chicken.tickCount * 0.15;
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

            if (chicken.tickCount % 10 == 0) {
                serverWorld.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    chicken.getX(), chicken.getY() + 1.5, chicken.getZ(),
                    5, 0.2, 0.2, 0.2, 0.02
                );
            }
        }

        // --- Boss Melodic hum ---
        if (chicken.tickCount % 40 == 0) {
            serverWorld.playSound(
                null,
                chicken.getX(), chicken.getY(), chicken.getZ(),
                net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value(),
                net.minecraft.sounds.SoundSource.HOSTILE,
                1.5f, 0.5f
            );
        }

        // --- Surrounding Block Control (Block Throwing) ---
        if (chicken.tickCount % 60 == 0) {
            Player targetPlayer = null;
            double nearestDistSq = Double.MAX_VALUE;
            for (Player player : serverWorld.players()) {
                if (!player.isCreative() && !player.isSpectator() && !player.isDeadOrDying()) {
                    double distSq = player.distanceToSqr(chicken);
                    if (distSq < 12.0 * 12.0 && distSq < nearestDistSq) {
                        nearestDistSq = distSq;
                        targetPlayer = player;
                    }
                }
            }

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

        java.util.List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(chicken);
        for (TraitType type : subTraits) {
            com.chaoschickens.fabric.trait.FabricTrait trait = ChaosChickensFabric.getTraitInstance(type);
            if (trait != null && trait.isPeriodic()) {
                trait.onTick(chicken);
            }
        }
    }

    @Override
    public void onDeath(Chicken chicken, net.minecraft.world.damagesource.DamageSource source) {
        if (chicken == null) return;
        clearBossBar(chicken.getUUID());

        if (ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            ((ServerLevel) chicken.level()).sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                chicken.getX(), chicken.getY(), chicken.getZ(),
                1, 0.0, 0.0, 0.0, 0.0);
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
}
