/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.common.trait;

/**
 * Enum representing all possible chaos chicken trait types.
 * Custom traits should use the CUSTOM prefix with a unique name.
 */
public enum TraitType {

    // No trait (normal chicken)
    EMPTY("empty", "Normal Chicken"),

    // Default traits
    EXPLOSIVE("explosive", "Explosive Chicken"),
    SPEED("speed", "Speed Chicken"),
    FIRE("fire", "Fire Chicken"),
    MAGNET("magnet", "Magnet Chicken"),
    GOLDEN("golden", "Golden Chicken"),
    DISCO("disco", "Disco Chicken"),
    ZOMBIE("zombie", "Zombie Chicken"),
    TELEPORT("teleport", "Teleport Chicken"),
    ICE("ice", "Ice Chicken"),
    CURSED("cursed", "Cursed Chicken"),

    // Boss trait (multiple traits combined)
    BOSS("boss", "Boss Chicken"),

    // Custom traits namespace
    CUSTOM("custom", "Custom Chicken");

    private final String key;
    private final String displayName;

    TraitType(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get a TraitType from its string key.
     *
     * @param key The string key (e.g., "explosive")
     * @return The matching TraitType, or EMPTY if not found
     */
    public static TraitType fromKey(String key) {
        if (key == null) return EMPTY;
        for (TraitType type : values()) {
            if (type.key.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return EMPTY;
    }
}
