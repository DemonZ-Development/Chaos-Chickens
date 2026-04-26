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
package com.chaoschickens.common.config;

/**
 * Config version tracker for migration support.
 * Tracks the config schema version and provides migration logic
 * when the config format changes between plugin versions.
 */
public final class ConfigVersion {

    /** Current config schema version. Increment when config format changes. */
    public static final int CURRENT_VERSION = 3;

    /** Config version key used in all config formats (YAML, JSON). */
    public static final String CONFIG_VERSION_KEY = "config-version";

    private ConfigVersion() {}

    /**
     * Check if a config needs migration by comparing its version to the current version.
     *
     * @param loadedVersion The version found in the loaded config file
     * @return true if migration is needed
     */
    public static boolean needsMigration(int loadedVersion) {
        return loadedVersion < CURRENT_VERSION;
    }

    /**
     * Get a description of what changed in each config version.
     *
     * @param version The config version number
     * @return Description of changes in that version
     */
    public static String getChangeDescription(int version) {
        switch (version) {
            case 1:
                return "Initial config format (v1.0.0)";
            case 2:
                return "Added config-version field, check-for-updates setting, " +
                       "enable-folia-support setting, and max-chickens-per-player enforcement";
            case 3:
                return "Added bstats-enabled setting, boss trait implementation";
            default:
                return "Unknown config version";
        }
    }
}
