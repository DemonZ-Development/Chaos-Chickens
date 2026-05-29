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
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Chicken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BossTraitTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(com.chaoschickens.bukkit.ChaosChickensBukkit.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testOnApplySetsHealth() {
        BossTrait trait = new BossTrait();
        World world = server.addSimpleWorld("world");
        Chicken chicken = world.spawn(new Location(world, 0, 64, 0), Chicken.class);
        trait.onApply(chicken);
        assertNotNull(chicken.getAttribute(Attribute.GENERIC_MAX_HEALTH));
        assertEquals(12.0, chicken.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue(), 0.001);
        assertEquals(12.0, chicken.getHealth(), 0.001);
    }

    @Test
    void testOnApplySetsName() {
        BossTrait trait = new BossTrait();
        World world = server.addSimpleWorld("world");
        Chicken chicken = world.spawn(new Location(world, 0, 64, 0), Chicken.class);
        trait.onApply(chicken);
        assertTrue(chicken.getCustomName().contains("BOSS"));
        assertTrue(chicken.isCustomNameVisible());
    }

    @Test
    void testIsHostile() {
        BossTrait trait = new BossTrait();
        assertTrue(trait.isHostile());
    }
}
