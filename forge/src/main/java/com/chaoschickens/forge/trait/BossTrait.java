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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BOSS chicken trait — the most powerful and dangerous chicken.
 * Combines multiple sub-traits, attacks with fireballs and blocks,
 * teleports on cooldown, has massive HP, and creates a huge explosion on death.
 */
public class BossTrait extends ForgeTrait {

    private static final Random RANDOM = new Random();
    /** Track last teleport time per boss to prevent teleport spam (10s cooldown). */
    private static final Map<UUID, Long> lastTeleportTime = new ConcurrentHashMap<>();

    /** Track local tick counter per boss chicken to guarantee correct tick timing. */
    private static final Map<UUID, Integer> bossTicks = new ConcurrentHashMap<>();

    public BossTrait() {
        super(TraitType.BOSS, "A powerful chicken with multiple chaos traits combined!", 0.02,
                true, true, 10); // Ticked frequently for sub-traits
    }

    @Override
    public boolean modifiesDrops() {
        return true;
    }

    @Override
    public void onModifyDrops(Chicken chicken, LivingDropsEvent event) {
        List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(chicken);
        for (TraitType type : subTraits) {
            ForgeTrait trait = ChaosChickensForge.getTraitInstance(type);
            if (trait != null && trait.modifiesDrops()) {
                trait.onModifyDrops(chicken, event);
            }
        }
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;

        // Load or roll sub-traits
        List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(chicken);
        if (subTraits.isEmpty()) {
            int count = ConfigLoader.getConfig().getBossTraitCount();
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
            ChickenDataUtil.setBossSubTraits(chicken, subTraits);
        }

        // Apply all sub-traits
        for (TraitType type : subTraits) {
            ForgeTrait trait = ChaosChickensForge.getTraitInstance(type);
            if (trait != null) {
                trait.onApply(chicken);
            }
        }

        // Boss name and massive health (override sub-trait names)
        chicken.setCustomName(Component.literal("☠ BOSS Chicken ☠")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        chicken.setCustomNameVisible(true);
        if (chicken.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) != null) {
            chicken.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(80.0);
            chicken.setHealth(80.0f);
        }
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isRemoved() || chicken.isDeadOrDying()) return;
        if (!(chicken.level() instanceof ServerLevel serverLevel)) return;

        UUID uuid = chicken.getUUID();
        int tick = bossTicks.merge(uuid, 10, Integer::sum);

        // === Boss aura particles (rotating trail) ===
        if (ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            double angle = tick * 0.15;
            double radius = 0.8;
            double px = chicken.getX() + Math.cos(angle) * radius;
            double pz = chicken.getZ() + Math.sin(angle) * radius;
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    px, chicken.getY() + 0.6, pz,
                    2, 0.05, 0.1, 0.05, 0.01);
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    px, chicken.getY() + 0.3, pz,
                    1, 0.05, 0.1, 0.05, 0.2);
        }

        // Soul fire flame aura every 10 ticks
        if (tick % 10 == 0 && ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    chicken.getX(), chicken.getY() + 0.5, chicken.getZ(),
                    3, 0.3, 0.2, 0.3, 0.01);
        }

        // === Boss teleport (10 second cooldown) ===
        long now = serverLevel.getGameTime();
        long lastTp = lastTeleportTime.getOrDefault(uuid, 0L);
        if (now - lastTp > 200) { // 200 ticks = 10 seconds
            ForgeTrait teleportTrait = ChaosChickensForge.getTraitInstance(TraitType.TELEPORT);
            if (teleportTrait instanceof TeleportTrait tp) {
                tp.teleport(chicken, serverLevel);
                lastTeleportTime.put(uuid, now);
            }
        }

        // === Fireball attack every 80 ticks ===
        if (tick % 80 == 0) {
            Player target = findNearestTarget(chicken, serverLevel, 16.0);
            if (target != null) {
                Vec3 direction = target.position().subtract(chicken.position()).normalize();
                try {
                    SmallFireball fireball = new SmallFireball(
                            serverLevel, chicken,
                            new Vec3(direction.x * 1.0, direction.y * 0.5 + 0.2, direction.z * 1.0));
                    fireball.setPos(chicken.getX(), chicken.getY() + 0.5, chicken.getZ());
                    serverLevel.addFreshEntity(fireball);
                } catch (Exception e) {
                    // Fallback: just do damage
                }
            }
        }

        // === Block throwing attack every 60 ticks ===
        if (tick % 60 == 0) {
            Player target = findNearestTarget(chicken, serverLevel, 12.0);
            if (target != null) {
                // Find a nearby solid block to throw
                BlockPos chickenPos = chicken.blockPosition();
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos checkPos = chickenPos.offset(dx, 0, dz);
                        BlockState state = serverLevel.getBlockState(checkPos);
                        if (state.isSolid() && !state.isAir()) {
                            try {
                                FallingBlockEntity fallingBlock = FallingBlockEntity.fall(
                                        serverLevel, checkPos, state);
                                if (fallingBlock != null) {
                                    Vec3 launchDir = target.position().subtract(fallingBlock.position()).normalize();
                                    fallingBlock.setDeltaMovement(launchDir.x * 0.8, 0.5 + launchDir.y * 0.3, launchDir.z * 0.8);
                                    fallingBlock.setHurtsEntities(2.0f, 40);
                                    fallingBlock.disableDrop();
                                }
                            } catch (Exception ignored) {}
                            break;
                        }
                    }
                }
            }
        }

        // === Boss melodic hum every 40 ticks ===
        if (tick % 40 == 0) {
            serverLevel.playSound(null, chicken.getX(), chicken.getY(), chicken.getZ(),
                    SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.HOSTILE,
                    1.5f, 0.4f + RANDOM.nextFloat() * 0.3f);
        }

        // === Lightning Strike Attack (every 120 ticks) ===
        if (tick % 120 == 0) {
            Player target = findNearestTarget(chicken, serverLevel, 16.0);
            if (target != null) {
                net.minecraft.world.entity.LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
                if (lightning != null) {
                    lightning.moveTo(target.getX(), target.getY(), target.getZ());
                    serverLevel.addFreshEntity(lightning);
                }
            }
        }

        // === Summon Zombie Chicken Minions (every 160 ticks) ===
        if (tick % 160 == 0) {
            java.util.List<Chicken> nearbyMinions = serverLevel.getEntitiesOfClass(
                    Chicken.class,
                    chicken.getBoundingBox().inflate(16.0),
                    otherChicken -> ChaosChickensForge.getActiveTrait(otherChicken).orElse(TraitType.EMPTY) == TraitType.ZOMBIE
            );
            if (nearbyMinions.size() < 6) {
                for (int i = 0; i < 2; i++) {
                    Chicken minion = EntityType.CHICKEN.create(serverLevel);
                    if (minion != null) {
                        minion.moveTo(
                            chicken.getX() + RANDOM.nextDouble() * 3.0 - 1.5,
                            chicken.getY(),
                            chicken.getZ() + RANDOM.nextDouble() * 3.0 - 1.5,
                            RANDOM.nextFloat() * 360.0f,
                            0.0f
                        );
                        ChaosChickensForge.assignTrait(minion, TraitType.ZOMBIE);
                        serverLevel.addFreshEntity(minion);
                    }
                }
                if (ConfigLoader.getConfig().isTraitParticlesEnabled()) {
                    serverLevel.sendParticles(ParticleTypes.WITCH,
                            chicken.getX(), chicken.getY() + 1.0, chicken.getZ(),
                            20, 1.0, 0.5, 1.0, 0.1);
                }
                serverLevel.playSound(null, chicken.getX(), chicken.getY(), chicken.getZ(),
                        SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 1.5f, 0.8f);
            }
        }

        // === Tick sub-traits (but SKIP teleport — boss handles teleport itself) ===
        List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(chicken);
        for (TraitType type : subTraits) {
            if (type == TraitType.TELEPORT) continue; // Boss handles its own teleport
            ForgeTrait trait = ChaosChickensForge.getTraitInstance(type);
            if (trait != null && trait.isPeriodic()) {
                trait.onTick(chicken);
            }
        }
    }

    @Override
    public void onDeath(Chicken chicken, DamageSource source) {
        if (chicken == null) return;
        if (!(chicken.level() instanceof ServerLevel serverLevel)) return;

        double x = chicken.getX(), y = chicken.getY(), z = chicken.getZ();

        // === MASSIVE DEATH EXPLOSION ===
        serverLevel.explode(chicken, x, y, z, 8.0f, true, Level.ExplosionInteraction.TNT);

        // === Epic death particles ===
        if (ConfigLoader.getConfig().isTraitParticlesEnabled()) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    x, y + 1.0, z, 3, 0.5, 0.5, 0.5, 0.0);

            // Soul fire ring
            for (int i = 0; i < 16; i++) {
                double angle = (i / 16.0) * Math.PI * 2;
                double rx = x + Math.cos(angle) * 3.0;
                double rz = z + Math.sin(angle) * 3.0;
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        rx, y + 0.5, rz, 5, 0.1, 0.5, 0.1, 0.05);
            }

            // Witch sparks
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    x, y + 2.0, z, 40, 2.0, 2.0, 2.0, 0.2);

            // Enchant swirl
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    x, y + 1.5, z, 30, 1.5, 1.5, 1.5, 0.1);
        }

        // === Epic death sounds ===
        serverLevel.playSound(null, x, y, z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0f, 0.5f);
        serverLevel.playSound(null, x, y, z,
                SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 1.5f, 0.8f);

        // === Loot drops ===
        ItemEntity netherStar = new ItemEntity(serverLevel, x, y + 0.5, z,
                new ItemStack(Items.NETHER_STAR));
        serverLevel.addFreshEntity(netherStar);

        int diamonds = 2 + RANDOM.nextInt(4);
        ItemEntity diamondDrop = new ItemEntity(serverLevel, x, y + 0.5, z,
                new ItemStack(Items.DIAMOND, diamonds));
        serverLevel.addFreshEntity(diamondDrop);

        // === Trigger death for all sub-traits ===
        List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(chicken);
        for (TraitType type : subTraits) {
            ForgeTrait trait = ChaosChickensForge.getTraitInstance(type);
            if (trait != null) {
                trait.onDeath(chicken, source);
            }
        }

        // Cleanup
        lastTeleportTime.remove(chicken.getUUID());
        bossTicks.remove(chicken.getUUID());
    }

    @Override
    public double getProximityRange() {
        return 16.0;
    }

    @Override
    public void onPlayerNear(Chicken chicken, Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));

        // Delegate proximity effects for all sub-traits
        List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(chicken);
        for (TraitType type : subTraits) {
            ForgeTrait trait = ChaosChickensForge.getTraitInstance(type);
            if (trait != null && trait.getProximityRange() > 0) {
                double distSq = chicken.distanceToSqr(player);
                if (distSq <= trait.getProximityRange() * trait.getProximityRange()) {
                    trait.onPlayerNear(chicken, player);
                }
            }
        }
    }

    /** Cleanup static map for removed bosses. */
    public static void cleanupBoss(UUID uuid) {
        lastTeleportTime.remove(uuid);
        bossTicks.remove(uuid);
    }

    private Player findNearestTarget(Chicken chicken, ServerLevel level, double range) {
        List<Player> players = level.getEntitiesOfClass(Player.class,
                chicken.getBoundingBox().inflate(range),
                p -> p.isAlive() && !p.isSpectator());
        Player nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Player p : players) {
            double dist = chicken.distanceToSqr(p);
            if (dist < minDist) {
                minDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }
}
