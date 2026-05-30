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
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main entry point for the Chaos Chickens Fabric mod.
 * Created and maintained by the DemonZ Development community.
 * Lead Developer: Cyrus
 *
 * Handles initialization, trait registration, and the server tick loop
 * for periodic trait processing.
 */
public class ChaosChickensFabric implements ModInitializer {

    public static final String MOD_ID = "chaoschickens";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Active chaos chickens tracked by entity UUID -> trait type. */
    private static final Map<UUID, TraitType> activeChickens = new ConcurrentHashMap<>();

    /** Tracks which chickens have already had onApply called (prevents re-execution on chunk reload). */
    private static final Set<UUID> appliedChickens = ConcurrentHashMap.newKeySet();

    /** Tracks which chickens have already been checked/evaluated for a trait. */
    private static final Set<UUID> checkedChickens = ConcurrentHashMap.newKeySet();

    public static void addCheckedChicken(UUID uuid) {
        checkedChickens.add(uuid);
    }

    public static boolean isCheckedChicken(UUID uuid) {
        return checkedChickens.contains(uuid);
    }

    /** Per-chicken tick counters for correct interval calculation. */
    private static final Map<UUID, Integer> chickenTickCounters = new ConcurrentHashMap<>();

    /** Global tick counter for throttling server tick processing. */
    private static volatile int globalTickCounter = 0;

    /** Cached trait instances for quick lookup. */
    private static final Map<TraitType, FabricTrait> traitInstances = new ConcurrentHashMap<>();

    /** Random instance for trait selection. */
    private static final Random RANDOM = new Random();

    @Override
    public void onInitialize() {
        LOGGER.info("Chaos Chickens initializing...");

        // Initialize API
        ChaosChickensAPI.initialize();

        // Register all traits
        registerTraits();

        // Load configuration
        ConfigLoader.load();

        // Register entity join event — assign traits to newly spawned chickens
        // NOTE: NBT loading is handled entirely by the ChickenEntityMixin write/read injects.
        // This handler only assigns traits to fresh (non-loaded) chickens.
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ChickenEntity chicken) {
                // If already tracked or checked, skip
                if (activeChickens.containsKey(chicken.getUuid()) || isCheckedChicken(chicken.getUuid())) return;

                // Mark as checked
                addCheckedChicken(chicken.getUuid());

                // Baby chickens always spawn with traits
                if (chicken.isBaby()) {
                    assignTrait(chicken, null);
                    return;
                }

                if (ConfigLoader.getConfig().isOnlyNaturalSpawns()) {
                    return;
                }

                // Check for max chickens per player limit
                int max = ConfigLoader.getConfig().getMaxChickensPerPlayer();
                if (max > -1) {
                    net.minecraft.server.network.ServerPlayerEntity nearestPlayer = null;
                    double nearestDist = Double.MAX_VALUE;
                    for (net.minecraft.server.network.ServerPlayerEntity player : world.getPlayers()) {
                        double dist = player.squaredDistanceTo(chicken.getX(), chicken.getY(), chicken.getZ());
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

                // Roll for chaos chance on fresh spawns
                double chaosChance = ConfigLoader.getConfig().getChaosChance();
                if (ConfigLoader.getConfig().isForceAllChickensToHaveTraits() || RANDOM.nextDouble() < chaosChance) {
                    assignTrait(chicken, null);
                }
            }
        });

        // Register server tick event — handle periodic traits
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher);
        });

        // Check for updates via Modrinth
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

    /**
     * Register all default chaos traits with the API.
     */
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

    /**
     * Register a single trait instance with both the API and local cache.
     */
    private void registerTrait(FabricTrait trait) {
        ChaosChickensAPI.registerTrait(trait, trait.getDefaultWeight());
        traitInstances.put(trait.getType(), trait);
    }

    public static void assignTrait(ChickenEntity chicken) {
        assignTrait(chicken, null);
    }

    /**
     * Assign a specific or random chaos trait to a chicken entity.
     *
     * @param chicken   The chicken to assign a trait to
     * @param traitType The trait type to assign, or null to pick randomly
     */
    public static void assignTrait(ChickenEntity chicken, TraitType traitType) {
        if (chicken == null || chicken.isRemoved()) return;

        if (traitType == null) {
            traitType = ChaosChickensAPI.pickRandomTrait(RANDOM);
        }
        if (traitType == TraitType.EMPTY) return;

        // Set trait on the chicken
        ChickenDataUtil.setTrait(chicken, traitType);
        activeChickens.put(chicken.getUuid(), traitType);

        // Apply trait effects
        FabricTrait trait = getTraitInstance(traitType);
        if (trait != null) {
            trait.onApply(chicken);
        }

        LOGGER.debug("Assigned trait {} to chicken at {}", traitType.getKey(), chicken.getBlockPos());
    }

    /**
     * Get the FabricTrait instance for a given trait type.
     *
     * @param type The trait type
     * @return The trait instance, or null if not registered
     */
    public static FabricTrait getTraitInstance(TraitType type) {
        return traitInstances.get(type);
    }

    /**
     * Get the trait type for an active chaos chicken.
     *
     * @param chicken The chicken entity
     * @return Optional containing the trait type
     */
    public static Optional<TraitType> getActiveTrait(ChickenEntity chicken) {
        if (chicken == null) return Optional.empty();
        return Optional.ofNullable(activeChickens.get(chicken.getUuid()));
    }

    /**
     * Remove a chicken from the active tracking map.
     *
     * @param uuid The chicken's entity UUID
     */
    public static void removeActiveChicken(UUID uuid) {
        activeChickens.remove(uuid);
        chickenTickCounters.remove(uuid);
        appliedChickens.remove(uuid);
    }

    private void onServerTick(MinecraftServer server) {
        globalTickCounter++;

        for (ServerWorld world : server.getWorlds()) {
            // Get all chicken entities in the world
            List<ChickenEntity> chickens = new ArrayList<>();
            for (net.minecraft.entity.Entity entity : world.iterateEntities()) {
                if (entity instanceof ChickenEntity chicken) {
                    if (activeChickens.containsKey(chicken.getUuid())) {
                        chickens.add(chicken);
                    }
                }
            }

            for (ChickenEntity chicken : chickens) {
                if (chicken.isRemoved() || chicken.isDead()) {
                    activeChickens.remove(chicken.getUuid());
                    chickenTickCounters.remove(chicken.getUuid());
                    appliedChickens.remove(chicken.getUuid());
                    continue;
                }

                TraitType traitType = activeChickens.get(chicken.getUuid());
                if (traitType == null) continue;

                // Increment per-chicken tick counter
                int ticks = chickenTickCounters.merge(chicken.getUuid(), 1, Integer::sum);

                FabricTrait trait = traitInstances.get(traitType);
                if (trait == null) continue;

                // Handle periodic tick using per-chicken counters
                if (trait.isPeriodic()) {
                    int tickInterval = trait.getTickInterval();
                    if (ticks % Math.max(1, tickInterval) == 0) {
                        try {
                            trait.onTick(chicken);
                        } catch (Exception e) {
                            LOGGER.error("Error ticking trait {} for chicken {}", traitType.getKey(), chicken.getUuid(), e);
                        }
                    }
                }

                // Handle proximity-based effects (every 20 game ticks)
                double range = trait.getProximityRange();
                if (range > 0 && ticks % 20 == 0) {
                    world.getPlayers().stream()
                            .filter(player -> !player.isDead())
                            .filter(player -> player.squaredDistanceTo(chicken) <= range * range)
                            .forEach(player -> {
                                try {
                                    trait.onPlayerNear(chicken, player);
                                } catch (Exception e) {
                                    LOGGER.error("Error in proximity check for trait {}", traitType.getKey(), e);
                                }
                            });
                }
            }
        }
    }

    /**
     * Get all active chaos chicken UUIDs. Used by mixins.
     *
     * @return Unmodifiable set of active chicken UUIDs
     */
    public static Set<UUID> getActiveChickenUUIDs() {
        return Collections.unmodifiableSet(activeChickens.keySet());
    }

    /**
     * Add a chicken to the active tracking map. Used by mixins on NBT load.
     *
     * @param uuid The chicken's entity UUID
     * @param type The trait type
     */
    public static void addActiveChicken(UUID uuid, TraitType type) {
        activeChickens.put(uuid, type);
        chickenTickCounters.put(uuid, 0);
        appliedChickens.add(uuid);
        addCheckedChicken(uuid);
    }

    public static int getActiveChickenCountForPlayer(net.minecraft.server.network.ServerPlayerEntity player) {
        int count = 0;
        net.minecraft.util.math.Vec3d playerPos = player.getPos();
        for (UUID uuid : activeChickens.keySet()) {
            net.minecraft.entity.Entity entity = player.getServerWorld().getEntity(uuid);
            if (entity instanceof ChickenEntity && entity.getWorld().equals(player.getWorld())
                    && entity.getPos().squaredDistanceTo(playerPos) < 128 * 128) {
                count++;
            }
        }
        return count;
    }

    private void registerCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.server.command.ServerCommandSource> dispatcher) {
        com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.server.command.ServerCommandSource> ccBuilder = net.minecraft.server.command.CommandManager.literal("chaoschickens")
            .requires(source -> source.hasPermissionLevel(2))
            .then(net.minecraft.server.command.CommandManager.literal("spawn")
                .then(net.minecraft.server.command.CommandManager.argument("trait", com.mojang.brigadier.arguments.StringArgumentType.word())
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
            )
            .then(net.minecraft.server.command.CommandManager.literal("give")
                .then(net.minecraft.server.command.CommandManager.argument("trait", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .suggests((context, builder) -> {
                        for (TraitType type : TraitType.values()) {
                            if (type != TraitType.EMPTY && type != TraitType.CUSTOM) {
                                builder.suggest(type.getKey());
                            }
                        }
                        return builder.buildFuture();
                    })
                    .executes(context -> giveEgg(context.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(context, "trait")))
                )
            );

        com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.server.command.ServerCommandSource> ccSpawnBuilder = net.minecraft.server.command.CommandManager.literal("ccspawn")
            .requires(source -> source.hasPermissionLevel(2))
            .then(net.minecraft.server.command.CommandManager.argument("trait", com.mojang.brigadier.arguments.StringArgumentType.word())
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

    private static int spawnChicken(net.minecraft.server.command.ServerCommandSource source, String traitKey) {
        TraitType traitType = TraitType.fromKey(traitKey);
        if (traitType == TraitType.EMPTY && !traitKey.equalsIgnoreCase("empty")) {
            source.sendError(net.minecraft.text.Text.literal("Unknown trait: " + traitKey));
            return 0;
        }

        net.minecraft.server.world.ServerWorld world = source.getWorld();
        net.minecraft.util.math.Vec3d pos = source.getPosition();

        ChickenEntity chicken = net.minecraft.entity.EntityType.CHICKEN.create(world);
        if (chicken == null) {
            source.sendError(net.minecraft.text.Text.literal("Failed to create chicken!"));
            return 0;
        }

        chicken.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0.0f, 0.0f);
        assignTrait(chicken, traitType);
        world.spawnEntity(chicken);

        source.sendFeedback(() -> net.minecraft.text.Text.literal("Spawned a chicken with " + traitType.getDisplayName() + " trait!"), true);
        return 1;
    }

    private static int giveEgg(net.minecraft.server.command.ServerCommandSource source, String traitKey) {
        TraitType traitType = TraitType.fromKey(traitKey);
        if (traitType == TraitType.EMPTY && !traitKey.equalsIgnoreCase("empty")) {
            source.sendError(net.minecraft.text.Text.literal("Unknown trait: " + traitKey));
            return 0;
        }

        try {
            net.minecraft.server.network.ServerPlayerEntity player = source.getPlayer();
            if (player == null) {
                source.sendError(net.minecraft.text.Text.literal("This command can only be run by a player!"));
                return 0;
            }

            String playerName = player.getName().getString();
            String cmd = String.format("give %s minecraft:chicken_spawn_egg[minecraft:entity_data={id:\"minecraft:chicken\",chaoschicken_trait:\"%s\"},minecraft:custom_name='\"Chaos Chicken Egg (%s)\"']",
                    playerName, traitType.getKey(), traitType.getDisplayName());
            
            source.getServer().getCommandManager().executeWithPrefix(source, cmd);
            source.sendFeedback(() -> net.minecraft.text.Text.literal("Gave a Chaos Chicken Egg (" + traitType.getDisplayName() + ") to " + playerName + "!"), true);
            return 1;
        } catch (Exception e) {
            source.sendError(net.minecraft.text.Text.literal("Error giving spawn egg: " + e.getMessage()));
            return 0;
        }
    }
}
