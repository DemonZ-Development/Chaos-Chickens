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
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GoldenTraitTest {

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
    void testOnApplySetsCustomNameAndGlowing() {
        GoldenTrait trait = new GoldenTrait();
        World world = server.addSimpleWorld("world");
        Chicken chicken = world.spawn(new Location(world, 0, 64, 0), Chicken.class);
        trait.onApply(chicken);
        assertTrue(chicken.getCustomName().contains("Golden"));
        assertTrue(chicken.isCustomNameVisible());
        assertTrue(chicken.isGlowing());
    }

    @Test
    void testOnDeathModifiesDrops() {
        GoldenTrait trait = new GoldenTrait();
        World world = server.addSimpleWorld("world");
        Chicken chicken = world.spawn(new Location(world, 0, 64, 0), Chicken.class);
        
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(org.bukkit.Material.FEATHER, 1));
        
        EntityDeathEvent event = new EntityDeathEvent(chicken, drops, 0);
        
        trait.onDeath(chicken, event);
        
        // Default drop (feather) should be cleared, replaced by one ore drop
        assertEquals(1, event.getDrops().size());
        assertNotEquals(org.bukkit.Material.FEATHER, event.getDrops().get(0).getType());
    }
}
