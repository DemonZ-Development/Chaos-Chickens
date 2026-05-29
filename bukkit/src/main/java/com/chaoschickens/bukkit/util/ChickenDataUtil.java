/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.bukkit.util;

import com.chaoschickens.common.trait.TraitType;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Chicken;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Utility class for reading and writing trait data on chicken entities
 * using Bukkit's PersistentDataContainer API.
 */
public final class ChickenDataUtil {

    private static final String KEY_NAMESPACE = "chaoschickens";
    private static final String KEY_NAME = "trait";
    private ChickenDataUtil() {
        // Utility class
    }

    public static NamespacedKey getTraitKey(JavaPlugin plugin) {
        return new NamespacedKey(plugin, KEY_NAME);
    }

    /**
     * Set the trait type on a chicken entity via PersistentDataContainer.
     *
     * @param plugin  The plugin instance
     * @param chicken The chicken entity
     * @param type    The trait type to assign
     */
    public static void setTrait(JavaPlugin plugin, Chicken chicken, TraitType type) {
        if (chicken == null || type == null) return;
        PersistentDataContainer pdc = chicken.getPersistentDataContainer();
        pdc.set(getTraitKey(plugin), PersistentDataType.STRING, type.getKey());
    }

    /**
     * Get the trait type from a chicken entity's PersistentDataContainer.
     *
     * @param plugin  The plugin instance
     * @param chicken The chicken entity
     * @return The TraitType, or EMPTY if no trait is stored
     */
    public static TraitType getTrait(JavaPlugin plugin, Chicken chicken) {
        if (chicken == null) return TraitType.EMPTY;
        PersistentDataContainer pdc = chicken.getPersistentDataContainer();
        NamespacedKey key = getTraitKey(plugin);
        if (!pdc.has(key, PersistentDataType.STRING)) {
            return TraitType.EMPTY;
        }
        String value = pdc.get(key, PersistentDataType.STRING);
        return TraitType.fromKey(value);
    }

    /**
     * Check if a chicken entity has a chaos trait stored.
     *
     * @param plugin  The plugin instance
     * @param chicken The chicken entity
     * @return true if the chicken has a non-EMPTY trait
     */
    public static boolean hasTrait(JavaPlugin plugin, Chicken chicken) {
        if (chicken == null) return false;
        PersistentDataContainer pdc = chicken.getPersistentDataContainer();
        return pdc.has(getTraitKey(plugin), PersistentDataType.STRING);
    }

    /**
     * Remove the trait data from a chicken entity.
     *
     * @param plugin  The plugin instance
     * @param chicken The chicken entity
     */
    public static void removeTrait(JavaPlugin plugin, Chicken chicken) {
        if (chicken == null) return;
        PersistentDataContainer pdc = chicken.getPersistentDataContainer();
        pdc.remove(getTraitKey(plugin));
    }

    public static void setBossSubTraits(JavaPlugin plugin, Chicken chicken, java.util.List<TraitType> types) {
        if (chicken == null || types == null) return;
        java.util.List<String> keys = new java.util.ArrayList<>();
        for (TraitType t : types) {
            keys.add(t.getKey());
        }
        String value = String.join(",", keys);
        NamespacedKey key = new NamespacedKey(plugin, "boss_sub_traits");
        chicken.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
    }

    public static java.util.List<TraitType> getBossSubTraits(JavaPlugin plugin, Chicken chicken) {
        java.util.List<TraitType> list = new java.util.ArrayList<>();
        if (chicken == null) return list;
        NamespacedKey key = new NamespacedKey(plugin, "boss_sub_traits");
        PersistentDataContainer pdc = chicken.getPersistentDataContainer();
        if (!pdc.has(key, PersistentDataType.STRING)) return list;
        String value = pdc.get(key, PersistentDataType.STRING);
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
