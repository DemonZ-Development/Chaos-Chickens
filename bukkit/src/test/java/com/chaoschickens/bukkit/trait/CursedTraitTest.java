/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms (at your option) any later version.
 */
package com.chaoschickens.bukkit.trait;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CursedTraitTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testOnApplySetsCustomName() {
        CursedTrait trait = new CursedTrait();
        World world = server.addSimpleWorld("world");
        Chicken chicken = world.spawn(new Location(world, 0, 64, 0), Chicken.class);
        trait.onApply(chicken);
        assertTrue(chicken.getCustomName().contains("Cursed"));
        assertTrue(chicken.isCustomNameVisible());
    }

    @Test
    void testOnPlayerNearAppliesPotionEffect() {
        CursedTrait trait = new CursedTrait();
        World world = server.addSimpleWorld("world");
        Chicken chicken = world.spawn(new Location(world, 0, 64, 0), Chicken.class);
        Player player = server.addPlayer();
        trait.onPlayerNear(chicken, player);
        assertFalse(player.getActivePotionEffects().isEmpty());
    }
}
