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
package com.chaoschickens.forge;

import com.chaoschickens.api.ChaosChickensAPI;
import com.chaoschickens.common.trait.TraitType;
import com.chaoschickens.common.util.UpdateChecker;
import com.chaoschickens.forge.trait.*;
import com.chaoschickens.forge.util.ChickenDataUtil;
import com.chaoschickens.forge.util.ConfigLoader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main entry point for the Chaos Chickens Forge mod.
 * Created and maintained by the DemonZ Development community.
 * Lead Developer: Cyrus
 *
 * Handles initialization, trait registration, event subscriptions,
 * and the server tick loop for periodic trait processing.
 */
@Mod(ChaosChickensForge.MOD_ID)
public class ChaosChickensForge {

    public static final String MOD_ID = "chaoschickens";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Active chaos chickens tracked by entity UUID -> trait type. */
    private static final Map<UUID, TraitType> activeChickens = new ConcurrentHashMap<>();

    /** Tracks which chickens have already had onApply called (prevents re-execution on chunk reload). */
    private static final Set<UUID> appliedChickens = ConcurrentHashMap.newKeySet();

    /** Per-chicken tick counters for correct interval calculation. */
    private static final Map<UUID, Integer> chickenTickCounters = new ConcurrentHashMap<>();

    /** Global tick counter for throttling server tick processing. */
    private static volatile int globalTickCounter = 0;

    /** Cached trait instances for quick lookup. */
    private static final Map<TraitType, ForgeTrait> traitInstances = new ConcurrentHashMap<>();

    /** Random instance for trait selection. */
    private static final Random RANDOM = new Random();

    public ChaosChickensForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Initialize API
        ChaosChickensAPI.initialize();

        // Register all traits
        registerTraits();

        // Load configuration
        ConfigLoader.load();

        // Register Forge event listeners
        MinecraftForge.EVENT_BUS.addListener(this::onEntityJoinLevel);
        MinecraftForge.EVENT_BUS.addListener(this::onEntityLeaveLevel);
        MinecraftForge.EVENT_BUS.addListener(this::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(this::onLivingDrops);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerInteract);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);

        // Check for updates via Modrinth
        String currentVersion = net.minecraftforge.fml.ModList.get()
                .getModContainerById(MOD_ID).get()
                .getModInfo().getVersion().toString();
        if (ConfigLoader.getConfig().isUpdateCheckingEnabled()) {
            UpdateChecker.setCurrentVersion(currentVersion);
            UpdateChecker.checkForUpdates();
            UpdateChecker.startScheduledUpdateChecks(5);
        }

        LOGGER.info("Chaos Chickens v{} (Forge) initialized! {} traits registered.", currentVersion, traitInstances.size());
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

    private void registerTrait(ForgeTrait trait) {
        ChaosChickensAPI.registerTrait(trait, trait.getDefaultWeight());
        traitInstances.put(trait.getType(), trait);
    }

    // ========== Event Handlers ==========

    private void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Chicken chicken)) return;

        if (ChickenDataUtil.hasTrait(chicken)) {
            TraitType existing = ChickenDataUtil.getTrait(chicken);
            if (existing != TraitType.EMPTY) {
                activeChickens.put(chicken.getUUID(), existing);
                chickenTickCounters.put(chicken.getUUID(), 0);
                // Only re-apply trait effects if not already applied this session
                if (appliedChickens.add(chicken.getUUID())) {
                    ForgeTrait trait = getTraitInstance(existing);
                    if (trait != null) {
                        trait.onApply(chicken);
                    }
                }
            }
            return;
        }

        // Avoid re-evaluating chunk-loaded chickens using persistent data compound
        if (chicken.getPersistentData().getBoolean("ChaosChickensChecked")) {
            return;
        }

        // If loaded from disk and has no trait, it's an existing normal chicken. Skip and mark as checked.
        if (event.loadedFromDisk()) {
            chicken.getPersistentData().putBoolean("ChaosChickensChecked", true);
            return;
        }

        // Mark as checked immediately
        chicken.getPersistentData().putBoolean("ChaosChickensChecked", true);

        // Baby chickens always spawn with traits
        if (chicken.isBaby()) {
            assignTrait(chicken, null);
            return;
        }

        if (ConfigLoader.getConfig().isOnlyNaturalSpawns()) {
            return;
        }

        // Enforce maxChickensPerPlayer limit
        int max = ConfigLoader.getConfig().getMaxChickensPerPlayer();
        if (max > -1) {
            net.minecraft.world.entity.player.Player nearestPlayer = null;
            double nearestDist = Double.MAX_VALUE;
            // Get players from the server level
            for (net.minecraft.world.entity.player.Player player : event.getLevel().players()) {
                double dist = player.distanceToSqr(chicken.getX(), chicken.getY(), chicken.getZ());
                if (dist < 64 * 64 && dist < nearestDist) {
                    nearestDist = dist;
                    nearestPlayer = player;
                }
            }
            if (nearestPlayer != null) {
                int count = getActiveChickenCountForPlayer(nearestPlayer);
                if (count >= max) {
                    return;
                }
            }
        }

        // Check for boss chicken first (independent roll)
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

    private void onEntityLeaveLevel(net.minecraftforge.event.entity.EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Chicken chicken)) return;
        
        removeActiveChicken(chicken.getUUID());
    }

    private void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Chicken chicken)) return;

        TraitType traitType = ChickenDataUtil.getTrait(chicken);
        if (traitType == TraitType.EMPTY) return;

        ForgeTrait trait = traitInstances.get(traitType);
        if (trait != null) {
            trait.onDeath(chicken, event.getSource());
        }
        removeActiveChicken(chicken.getUUID());
    }

    private void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Chicken chicken)) return;

        TraitType traitType = ChickenDataUtil.getTrait(chicken);
        if (traitType == TraitType.EMPTY) return;

        ForgeTrait trait = traitInstances.get(traitType);
        if (trait != null && trait.modifiesDrops()) {
            trait.onModifyDrops(chicken, event);
        }
    }

    private void onPlayerInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getTarget() instanceof Chicken chicken)) return;
        if (!event.getEntity().getMainHandItem().isEmpty()) return;

        TraitType traitType = activeChickens.get(chicken.getUUID());
        if (traitType == null) return;

        ForgeTrait trait = traitInstances.get(traitType);
        if (trait != null) {
            event.getEntity().sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "This chicken has the " + trait.getDisplayName() + " trait! " + trait.getDescription()
                    ).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE)
            );
        }
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        globalTickCounter++;
        // Only process every 10 ticks (0.5 seconds) for performance
        if (globalTickCounter % 10 != 0) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // Group active chickens by level to avoid iterating all levels per UUID
        Map<net.minecraft.world.level.Level, List<Chicken>> chickensByLevel = new HashMap<>();
        for (UUID uuid : activeChickens.keySet()) {
            for (var level : server.getAllLevels()) {
                Entity entity = level.getEntity(uuid);
                if (entity instanceof Chicken chicken) {
                    chickensByLevel.computeIfAbsent(level, k -> new ArrayList<>()).add(chicken);
                    break;
                }
            }
        }

        for (var entry : chickensByLevel.entrySet()) {
            var level = entry.getKey();
            var chickens = entry.getValue();

            for (Chicken chicken : chickens) {
                if (chicken.isRemoved() || chicken.isDeadOrDying()) {
                    activeChickens.remove(chicken.getUUID());
                    chickenTickCounters.remove(chicken.getUUID());
                    appliedChickens.remove(chicken.getUUID());
                    continue;
                }

                TraitType traitType = activeChickens.get(chicken.getUUID());
                if (traitType == null) continue;

                // Increment per-chicken tick counter
                int ticks = chickenTickCounters.merge(chicken.getUUID(), 1, Integer::sum);

                ForgeTrait trait = traitInstances.get(traitType);
                if (trait == null) continue;

                // Handle periodic tick using per-chicken counters
                if (trait.isPeriodic()) {
                    int tickInterval = trait.getTickInterval();
                    // Task fires every 10 game ticks, so game ticks elapsed = ticks * 10
                    if ((ticks * 10) % Math.max(10, tickInterval) == 0) {
                        try {
                            trait.onTick(chicken);
                        } catch (Exception e) {
                            LOGGER.error("Error ticking trait {} for chicken {}", traitType.getKey(), chicken.getUUID(), e);
                        }
                    }
                }

                // Handle proximity-based effects (every 2 global cycles = 20 game ticks)
                double range = trait.getProximityRange();
                if (range > 0 && ticks % 2 == 0) {
                    var players = level.getEntitiesOfClass(
                            Player.class,
                            chicken.getBoundingBox().inflate(range),
                            player -> player.isAlive() && !player.isSpectator() && !player.isCreative()
                    );
                    for (var player : players) {
                        try {
                            trait.onPlayerNear(chicken, player);
                        } catch (Exception e) {
                            LOGGER.error("Error in proximity check for trait {}", traitType.getKey(), e);
                        }
                    }
                }
            }
        }
    }

    // ========== Public API ==========

    public static void assignTrait(Chicken chicken) {
        assignTrait(chicken, null);
    }

    public static void assignTrait(Chicken chicken, TraitType traitType) {
        if (chicken == null || chicken.isRemoved()) return;

        if (traitType == null) {
            traitType = ChaosChickensAPI.pickRandomTrait(RANDOM);
        }
        if (traitType == TraitType.EMPTY) return;

        ChickenDataUtil.setTrait(chicken, traitType);
        activeChickens.put(chicken.getUUID(), traitType);

        ForgeTrait trait = getTraitInstance(traitType);
        if (trait != null) {
            trait.onApply(chicken);
        }

        LOGGER.debug("Assigned trait {} to chicken at {}", traitType.getKey(), chicken.blockPosition());
    }

    public static ForgeTrait getTraitInstance(TraitType type) {
        return traitInstances.get(type);
    }

    public static Optional<TraitType> getActiveTrait(Chicken chicken) {
        if (chicken == null) return Optional.empty();
        return Optional.ofNullable(activeChickens.get(chicken.getUUID()));
    }

    public static void removeActiveChicken(UUID uuid) {
        activeChickens.remove(uuid);
        chickenTickCounters.remove(uuid);
        appliedChickens.remove(uuid);
    }

    public static void addActiveChicken(UUID uuid, TraitType type) {
        activeChickens.put(uuid, type);
        chickenTickCounters.put(uuid, 0);
        appliedChickens.add(uuid);
    }

    public static int getActiveChickenCountForPlayer(net.minecraft.world.entity.player.Player player) {
        int count = 0;
        net.minecraft.world.phys.Vec3 playerPos = player.position();
        for (UUID uuid : activeChickens.keySet()) {
            net.minecraft.world.entity.Entity entity = null;
            if (player.getServer() != null) {
                for (net.minecraft.server.level.ServerLevel level : player.getServer().getAllLevels()) {
                    entity = level.getEntity(uuid);
                    if (entity != null) break;
                }
            }
            if (entity instanceof Chicken && entity.level().equals(player.level())
                    && entity.position().distanceToSqr(playerPos) < 128 * 128) {
                count++;
            }
        }
        return count;
    }

    private void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        registerCommands(event.getDispatcher());
    }

    private void registerCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> ccBuilder = net.minecraft.commands.Commands.literal("chaoschickens")
            .requires(source -> source.hasPermission(2))
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
            .requires(source -> source.hasPermission(2))
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

        Chicken chicken = net.minecraft.world.entity.EntityType.CHICKEN.create(level);
        if (chicken == null) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("Failed to create chicken!"));
            return 0;
        }

        chicken.moveTo(pos.x, pos.y, pos.z, 0.0f, 0.0f);
        assignTrait(chicken, traitType);
        level.addFreshEntity(chicken);

        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("Spawned a chicken with " + traitType.getDisplayName() + " trait!"), true);
        return 1;
    }
}
