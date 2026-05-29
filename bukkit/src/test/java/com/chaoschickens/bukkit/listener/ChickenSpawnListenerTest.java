/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.bukkit.listener;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.chaoschickens.bukkit.ChaosChickensBukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Chicken;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChickenSpawnListenerTest {

    private ServerMock server;
    private ChaosChickensBukkit plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(ChaosChickensBukkit.class);
        plugin.getConfigManager().setChaosChance(1.0);
        plugin.getConfigManager().setBossChance(0.0);
        plugin.getConfigManager().setOnlyNaturalSpawns(false);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testChickenSpawnTriggersTraitAssignment() {
        ChickenSpawnListener listener = new ChickenSpawnListener(plugin);
        World world = server.addSimpleWorld("world");
        Chicken chicken = world.spawn(new Location(world, 0, 64, 0), Chicken.class);
        CreatureSpawnEvent event = new CreatureSpawnEvent(
                chicken, CreatureSpawnEvent.SpawnReason.NATURAL);
        listener.onCreatureSpawn(event);
        assertTrue(plugin.getActiveChickenCount() > 0);
    }
}
