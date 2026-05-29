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
import com.chaoschickens.bukkit.ChaosChickensBukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZombieTraitTest {

    private ServerMock server;
    private ChaosChickensBukkit plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(ChaosChickensBukkit.class);
        BukkitTrait.setPlugin(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testOnApplySetsCustomName() {
        ZombieTrait trait = new ZombieTrait();
        World world = server.addSimpleWorld("world");
        Chicken chicken = world.spawn(new Location(world, 0, 64, 0), Chicken.class);
        trait.onApply(chicken);
        assertTrue(chicken.getCustomName().contains("Zombie"));
        assertTrue(chicken.isCustomNameVisible());
    }

    @Test
    void testOnTickChasesAndAttacksPlayer() {
        ZombieTrait trait = new ZombieTrait();
        World world = server.addSimpleWorld("world");
        Chicken chicken = world.spawn(new Location(world, 0, 64, 0), Chicken.class);
        Player player = server.addPlayer();
        player.teleport(new Location(world, 0.5, 64, 0.5)); // teleport close to chicken
        
        double initialHealth = player.getHealth();
        trait.onTick(chicken);
        
        // Player should take damage since they are within contact range
        assertTrue(player.getHealth() < initialHealth);
    }
}
