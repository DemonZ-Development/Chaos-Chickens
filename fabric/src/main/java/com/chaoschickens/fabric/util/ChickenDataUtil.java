/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.fabric.util;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.entity.passive.ChickenEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChickenDataUtil {
    public static final String NBT_KEY = "chaoschicken_trait";

    private static final Map<UUID, String> pendingTraits = new ConcurrentHashMap<>();

    private ChickenDataUtil() {}

    public static void setTrait(ChickenEntity chicken, TraitType type) {
        if (chicken == null || type == null) return;
        pendingTraits.put(chicken.getUuid(), type.getKey());
    }

    public static boolean hasTrait(ChickenEntity chicken) {
        if (chicken == null) return false;
        return pendingTraits.containsKey(chicken.getUuid());
    }

    public static TraitType getTrait(ChickenEntity chicken) {
        if (chicken == null) return TraitType.EMPTY;
        String key = pendingTraits.get(chicken.getUuid());
        return key != null ? TraitType.fromKey(key) : TraitType.EMPTY;
    }

    public static void removeTrait(ChickenEntity chicken) {
        if (chicken != null) pendingTraits.remove(chicken.getUuid());
    }

    public static boolean hasPendingTrait(UUID uuid) {
        return pendingTraits.containsKey(uuid);
    }

    public static TraitType getAndClearPending(UUID uuid) {
        String key = pendingTraits.remove(uuid);
        return key != null ? TraitType.fromKey(key) : TraitType.EMPTY;
    }

    private static final Map<UUID, java.util.List<TraitType>> bossSubTraits = new ConcurrentHashMap<>();

    public static void setBossSubTraits(ChickenEntity chicken, java.util.List<TraitType> subTraits) {
        if (chicken == null || subTraits == null) return;
        bossSubTraits.put(chicken.getUuid(), subTraits);
    }

    public static java.util.List<TraitType> getBossSubTraits(ChickenEntity chicken) {
        if (chicken == null) return new java.util.ArrayList<>();
        return bossSubTraits.getOrDefault(chicken.getUuid(), new java.util.ArrayList<>());
    }

    public static void removeBossSubTraits(ChickenEntity chicken) {
        if (chicken != null) bossSubTraits.remove(chicken.getUuid());
    }
}
