/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.forge.util;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Chicken;

/**
 * Utility class for reading and writing trait data on chicken entities
 * using Forge NBT tags. Trait data persists across server restarts.
 */
public final class ChickenDataUtil {

    public static final String NBT_KEY = "chaoschicken_trait";

    private ChickenDataUtil() {}

    public static void setTrait(Chicken chicken, TraitType type) {
        if (chicken == null || type == null) return;
        chicken.getPersistentData().putString(NBT_KEY, type.getKey());
    }

    public static TraitType getTrait(Chicken chicken) {
        if (chicken == null) return TraitType.EMPTY;
        CompoundTag persistentData = chicken.getPersistentData();
        if (!persistentData.contains(NBT_KEY, CompoundTag.TAG_STRING)) return TraitType.EMPTY;
        return TraitType.fromKey(persistentData.getString(NBT_KEY));
    }

    public static boolean hasTrait(Chicken chicken) {
        if (chicken == null) return false;
        return chicken.getPersistentData().contains(NBT_KEY, CompoundTag.TAG_STRING);
    }

    public static void removeTrait(Chicken chicken) {
        if (chicken == null) return;
        chicken.getPersistentData().remove(NBT_KEY);
    }

    public static void setBossSubTraits(Chicken chicken, java.util.List<TraitType> types) {
        if (chicken == null || types == null) return;
        java.util.List<String> keys = new java.util.ArrayList<>();
        for (TraitType t : types) {
            keys.add(t.getKey());
        }
        String value = String.join(",", keys);
        chicken.getPersistentData().putString("boss_sub_traits", value);
    }

    public static java.util.List<TraitType> getBossSubTraits(Chicken chicken) {
        java.util.List<TraitType> list = new java.util.ArrayList<>();
        if (chicken == null) return list;
        CompoundTag persistentData = chicken.getPersistentData();
        if (!persistentData.contains("boss_sub_traits", CompoundTag.TAG_STRING)) return list;
        String value = persistentData.getString("boss_sub_traits");
        if (value == null || value.isEmpty()) return list;
        for (String s : value.split(",")) {
            TraitType t = TraitType.fromKey(s.trim());
            if (t != TraitType.EMPTY && t != TraitType.BOSS) {
                list.add(t);
            }
        }
        return list;
    }
}
