/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.common.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {

    @Test
    void testDefaultChaosChance() {
        ConfigManager config = new ConfigManager();
        assertEquals(0.35, config.getChaosChance(), 0.001);
    }

    @Test
    void testChaosChanceClampLow() {
        ConfigManager config = new ConfigManager();
        config.setChaosChance(-0.5);
        assertEquals(0.0, config.getChaosChance(), 0.001);
    }

    @Test
    void testChaosChanceClampHigh() {
        ConfigManager config = new ConfigManager();
        config.setChaosChance(1.5);
        assertEquals(1.0, config.getChaosChance(), 0.001);
    }

    @Test
    void testDefaultBossChance() {
        ConfigManager config = new ConfigManager();
        assertEquals(0.02, config.getBossChance(), 0.001);
    }

    @Test
    void testBossChanceClamp() {
        ConfigManager config = new ConfigManager();
        config.setBossChance(2.0);
        assertEquals(1.0, config.getBossChance(), 0.001);
    }

    @Test
    void testBossTraitCountClampLow() {
        ConfigManager config = new ConfigManager();
        config.setBossTraitCount(0);
        assertEquals(1, config.getBossTraitCount());
    }

    @Test
    void testBossTraitCountClampHigh() {
        ConfigManager config = new ConfigManager();
        config.setBossTraitCount(20);
        assertEquals(10, config.getBossTraitCount());
    }

    @Test
    void testTraitEnabledDefault() {
        ConfigManager config = new ConfigManager();
        assertTrue(config.isTraitEnabled("explosive"));
        assertFalse(config.isTraitEnabled("nonexistent"));
    }

    @Test
    void testSetTraitEnabled() {
        ConfigManager config = new ConfigManager();
        config.setTraitEnabled("explosive", false);
        assertFalse(config.isTraitEnabled("explosive"));
    }

    @Test
    void testSetTraitWeightClamps() {
        ConfigManager config = new ConfigManager();
        config.setTraitWeight("explosive", -5.0);
        assertEquals(0.0, config.getTraitWeight("explosive"), 0.001);
    }

    @Test
    void testDefaultConfigVersion() {
        ConfigManager config = new ConfigManager();
        assertEquals(ConfigVersion.CURRENT_VERSION, config.getConfigVersion());
    }

    @Test
    void testDefaultBossChickensEnabled() {
        ConfigManager config = new ConfigManager();
        assertTrue(config.isBossChickensEnabled());
    }

    @Test
    void testDefaultTraitParticles() {
        ConfigManager config = new ConfigManager();
        assertTrue(config.isTraitParticlesEnabled());
    }

    @Test
    void testDefaultTraitMessages() {
        ConfigManager config = new ConfigManager();
        assertTrue(config.isTraitMessagesEnabled());
    }

    @Test
    void testDefaultOnlyNaturalSpawns() {
        ConfigManager config = new ConfigManager();
        assertFalse(config.isOnlyNaturalSpawns());
    }

    @Test
    void testDefaultMaxChickensPerPlayer() {
        ConfigManager config = new ConfigManager();
        assertEquals(-1, config.getMaxChickensPerPlayer());
    }

    @Test
    void testApplyToAPI() {
        com.chaoschickens.api.ChaosChickensAPI.reset();
        ConfigManager config = new ConfigManager();
        config.setTraitEnabled("explosive", false);
        config.applyToAPI();
        assertFalse(com.chaoschickens.api.ChaosChickensAPI.isTraitEnabled(
                com.chaoschickens.common.trait.TraitType.EXPLOSIVE));
    }
}
