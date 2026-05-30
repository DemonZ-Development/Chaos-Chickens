/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * Lead Developer: Cyrus
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.fabric;

import com.chaoschickens.api.ChaosChickensAPI;
import com.chaoschickens.common.trait.TraitType;
import com.chaoschickens.common.util.UpdateChecker;
import com.chaoschickens.fabric.trait.*;
import com.chaoschickens.fabric.util.ChickenDataUtil;
import com.chaoschickens.fabric.util.ConfigLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChaosChickensFabric implements ModInitializer {

    public static final String MOD_ID = "chaoschickens";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Map<UUID, TraitType> activeChickens = new ConcurrentHashMap<>();
    private static final Set<UUID> appliedChickens = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> checkedChickens = ConcurrentHashMap.newKeySet();

    public static void addCheckedChicken(UUID uuid) { checkedChickens.add(uuid); }
    public static boolean isCheckedChicken(UUID uuid) { return checkedChickens.contains(uuid); }

    private static final Map<UUID, Integer> chickenTickCounters = new ConcurrentHashMap<>();
    private static volatile int globalTickCounter = 0;
    private static final Map<TraitType, FabricTrait> traitInstances = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();

    public static class ScheduledBlockRestore {
        public final ServerLevel level;
        public final net.minecraft.core.BlockPos pos;
        public final net.minecraft.world.level.block.state.BlockState originalState;
        public final long restoreTick;

        public ScheduledBlockRestore(ServerLevel level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState originalState, long restoreTick) {
            this.level = level;
            this.pos = pos;
            this.originalState = originalState;
            this.restoreTick = restoreTick;
        }
    }

    private static final List<ScheduledBlockRestore> blockRestorations = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void registerBlockRestore(ServerLevel level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState original, long delayTicks) {
        blockRestorations.add(new ScheduledBlockRestore(level, pos, original, globalTickCounter + delayTicks));
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Chaos Chickens initializing...");
        ChaosChickensAPI.initialize();
        registerTraits();
        ConfigLoader.load();

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof Chicken chicken) {
                if (activeChickens.containsKey(chicken.getUUID()) || isCheckedChicken(chicken.getUUID())) return;
                addCheckedChicken(chicken.getUUID());

                // Baby chickens always spawn with traits
                if (chicken.isBaby()) {
                    assignTrait(chicken, null);
                    return;
                }

                if (ConfigLoader.getConfig().isOnlyNaturalSpawns()) { return; }

                int max = ConfigLoader.getConfig().getMaxChickensPerPlayer();
                if (max > -1) {
                    net.minecraft.server.level.ServerPlayer nearestPlayer = null;
                    double nearestDist = Double.MAX_VALUE;
                    for (net.minecraft.server.level.ServerPlayer player : world.players()) {
                        double dist = player.distanceToSqr(chicken.getX(), chicken.getY(), chicken.getZ());
                        if (dist < 64 * 64 && dist < nearestDist) {
                            nearestDist = dist;
                            nearestPlayer = player;
                        }
                    }
                    if (nearestPlayer != null) {
                        int count = getActiveChickenCountForPlayer(nearestPlayer);
                        if (count >= max) { return; }
                    }
                }

                if (ConfigLoader.getConfig().isBossChickensEnabled()
                        && RANDOM.nextDouble() < ConfigLoader.getConfig().getBossChance()) {
                    assignTrait(chicken, TraitType.BOSS);
                    return;
                }

                double chaosChance = ConfigLoader.getConfig().getChaosChance();
                if (RANDOM.nextDouble() < chaosChance) {
                    assignTrait(chicken, null);
                }
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher);
        });

        String version = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getModContainer(MOD_ID).get()
                .getMetadata().getVersion().getFriendlyString();
        if (ConfigLoader.getConfig().isUpdateCheckingEnabled()) {
            UpdateChecker.setCurrentVersion(version);
            UpdateChecker.checkForUpdates();
            UpdateChecker.startScheduledUpdateChecks(5);
        }

        LOGGER.info("Chaos Chickens v{} initialized! {} traits registered.", version, traitInstances.size());
    }

    private void registerTraits() {
        registerTrait(new ExplosiveTrait());
        registerTrait(new SpeedTrait());
        registerTrait(new FireTrait());
        registerTrait(new MagnetTrait());
        registerTrait(new GoldenTrait());
        registerTrait(new DiscoTrait());
        registerTrait(new ZombieTrait());
        registerTrait(new TeleportTrait());
        registerTrait(new IceTrait());
        registerTrait(new CursedTrait());
        registerTrait(new BossTrait());
    }

    private void registerTrait(FabricTrait trait) {
        ChaosChickensAPI.registerTrait(trait, trait.getDefaultWeight());
        traitInstances.put(trait.getType(), trait);
    }

    public static void assignTrait(Chicken chicken) { assignTrait(chicken, null); }

    public static void assignTrait(Chicken chicken, TraitType traitType) {
        if (chicken == null || chicken.isRemoved()) return;
        if (traitType == null) { traitType = ChaosChickensAPI.pickRandomTrait(RANDOM); }
        if (traitType == TraitType.EMPTY) return;

        ChickenDataUtil.setTrait(chicken, traitType);
        activeChickens.put(chicken.getUUID(), traitType);

        FabricTrait trait = getTraitInstance(traitType);
        if (trait != null) { trait.onApply(chicken); }

        LOGGER.debug("Assigned trait {} to chicken at {}", traitType.getKey(), chicken.blockPosition());
    }

    public static FabricTrait getTraitInstance(TraitType type) { return traitInstances.get(type); }

    public static Optional<TraitType> getActiveTrait(Chicken chicken) {
        if (chicken == null) return Optional.empty();
        return Optional.ofNullable(activeChickens.get(chicken.getUUID()));
    }

    public static void removeActiveChicken(UUID uuid) {
        activeChickens.remove(uuid);
        chickenTickCounters.remove(uuid);
        appliedChickens.remove(uuid);
        com.chaoschickens.fabric.trait.BossTrait.clearBossBar(uuid);
    }

    private void onServerTick(MinecraftServer server) {
        globalTickCounter++;

        for (ScheduledBlockRestore restore : blockRestorations) {
            if (globalTickCounter >= restore.restoreTick) {
                restore.level.setBlockAndUpdate(restore.pos, restore.originalState);
                blockRestorations.remove(restore);
            }
        }

        if (globalTickCounter % 10 != 0) return;

        for (ServerLevel world : server.getAllLevels()) {
            List<Chicken> chickens = new ArrayList<>();
            for (net.minecraft.world.entity.Entity entity : world.getAllEntities()) {
                if (entity instanceof Chicken chicken) {
                    if (activeChickens.containsKey(chicken.getUUID())) { chickens.add(chicken); }
                }
            }

            for (Chicken chicken : chickens) {
                if (chicken.isRemoved() || chicken.isDeadOrDying()) {
                    activeChickens.remove(chicken.getUUID());
                    chickenTickCounters.remove(chicken.getUUID());
                    appliedChickens.remove(chicken.getUUID());
                    continue;
                }

                TraitType traitType = activeChickens.get(chicken.getUUID());
                if (traitType == null) continue;

                int ticks = chickenTickCounters.merge(chicken.getUUID(), 1, Integer::sum);

                FabricTrait trait = traitInstances.get(traitType);
                if (trait == null) continue;

                if (trait.isPeriodic()) {
                    int tickInterval = trait.getTickInterval();
                    if ((ticks * 10) % Math.max(1, tickInterval) == 0) {
                        try { trait.onTick(chicken); }
                        catch (Exception e) { LOGGER.error("Error ticking trait {} for chicken {}", traitType.getKey(), chicken.getUUID(), e); }
                    }
                }

                double range = trait.getProximityRange();
                if (range > 0 && ticks % 2 == 0) {
                    world.players().stream()
                            .filter(player -> !player.isDeadOrDying())
                            .filter(player -> player.distanceToSqr(chicken) <= range * range)
                            .forEach(player -> {
                                try { trait.onPlayerNear(chicken, player); }
                                catch (Exception e) { LOGGER.error("Error in proximity check for trait {}", traitType.getKey(), e); }
                            });
                }
            }
        }
    }

    public static Set<UUID> getActiveChickenUUIDs() { return Collections.unmodifiableSet(activeChickens.keySet()); }

    public static void addActiveChicken(UUID uuid, TraitType type) {
        activeChickens.put(uuid, type);
        chickenTickCounters.put(uuid, 0);
        appliedChickens.add(uuid);
        addCheckedChicken(uuid);
    }

    public static int getActiveChickenCountForPlayer(net.minecraft.server.level.ServerPlayer player) {
        int count = 0;
        net.minecraft.world.phys.Vec3 playerPos = player.position();
        for (UUID uuid : activeChickens.keySet()) {
            net.minecraft.world.entity.Entity entity = player.level().getEntity(uuid);
            if (entity instanceof Chicken && entity.level().equals(player.level())
                    && entity.position().distanceToSqr(playerPos) < 128 * 128) {
                count++;
            }
        }
        return count;
    }

    private void registerCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> ccBuilder = net.minecraft.commands.Commands.literal("chaoschickens")
            .requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
            .then(net.minecraft.commands.Commands.literal("spawn")
                .then(net.minecraft.commands.Commands.argument("trait", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .suggests((context, builder) -> {
                        for (TraitType type : TraitType.values()) {
                            if (type != TraitType.EMPTY && type != TraitType.CUSTOM) {
                                builder.suggest(type.getKey());
                            }
                        }
                        return builder.buildFuture();
                    })
                    .executes(context -> spawnChicken(context.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(context, "trait")))
                )
            );

        com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> ccSpawnBuilder = net.minecraft.commands.Commands.literal("ccspawn")
            .requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
            .then(net.minecraft.commands.Commands.argument("trait", com.mojang.brigadier.arguments.StringArgumentType.word())
                .suggests((context, builder) -> {
                    for (TraitType type : TraitType.values()) {
                        if (type != TraitType.EMPTY && type != TraitType.CUSTOM) {
                            builder.suggest(type.getKey());
                        }
                    }
                    return builder.buildFuture();
                })
                .executes(context -> spawnChicken(context.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(context, "trait")))
            );

        dispatcher.register(ccBuilder);
        dispatcher.register(ccSpawnBuilder);
    }

    private static int spawnChicken(net.minecraft.commands.CommandSourceStack source, String traitKey) {
        TraitType traitType = TraitType.fromKey(traitKey);
        if (traitType == TraitType.EMPTY && !traitKey.equalsIgnoreCase("empty")) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("Unknown trait: " + traitKey));
            return 0;
        }

        net.minecraft.server.level.ServerLevel level = source.getLevel();
        net.minecraft.world.phys.Vec3 pos = source.getPosition();

        Chicken chicken = net.minecraft.world.entity.EntityType.CHICKEN.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (chicken == null) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("Failed to create chicken!"));
            return 0;
        }

        chicken.setPos(pos.x, pos.y, pos.z);
        assignTrait(chicken, traitType);
        level.addFreshEntity(chicken);

        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("Spawned a chicken with " + traitType.getDisplayName() + " trait!"), true);
        return 1;
    }
}
