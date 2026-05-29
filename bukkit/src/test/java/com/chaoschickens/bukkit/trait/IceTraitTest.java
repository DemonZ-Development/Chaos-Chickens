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
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Chicken;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IceTraitTest {

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
        IceTrait trait = new IceTrait();
        World world = server.addSimpleWorld("world");
        Chicken chicken = world.spawn(new Location(world, 0, 64, 0), Chicken.class);
        trait.onApply(chicken);
        assertTrue(chicken.getCustomName().contains("Ice"));
        assertTrue(chicken.isCustomNameVisible());
    }

    @Test
    void testOnTickFreezesWater() {
        IceTrait trait = new IceTrait();
        World world = server.addSimpleWorld("world");
        Chicken chicken = world.spawn(new Location(world, 0, 64, 0), Chicken.class);
        
        Block blockBelow = chicken.getLocation().clone().subtract(0, 1, 0).getBlock();
        blockBelow.setType(Material.WATER);
        
        trait.onTick(chicken);
        
        assertEquals(Material.ICE, blockBelow.getType());
    }

    @Test
    void testOnDeathDropsIce() {
        IceTrait trait = new IceTrait();
        World world = server.addSimpleWorld("world");
        Chicken chicken = world.spawn(new Location(world, 0, 64, 0), Chicken.class);
        
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(Material.EGG, 1));
        drops.add(new ItemStack(Material.FEATHER, 1));
        
        EntityDeathEvent event = new EntityDeathEvent(chicken, drops, 0);
        trait.onDeath(chicken, event);
        
        // Egg should be replaced by Ice
        assertTrue(event.getDrops().stream().anyMatch(item -> item.getType() == Material.ICE));
        assertFalse(event.getDrops().stream().anyMatch(item -> item.getType() == Material.EGG));
        assertTrue(event.getDrops().stream().anyMatch(item -> item.getType() == Material.FEATHER));
    }
}
