/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.bukkit.command;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.chaoschickens.bukkit.ChaosChickensBukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChaosCommandTest {

    private ServerMock server;
    private ChaosChickensBukkit plugin;
    private ChaosCommand command;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(ChaosChickensBukkit.class);
        command = new ChaosCommand(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testReload() {
        Player player = server.addPlayer();
        Command cmd = plugin.getCommand("chaoschickens");
        assertNotNull(cmd);
        assertTrue(command.onCommand(player, cmd, "chaoschickens", new String[]{"reload"}));
    }

    @Test
    void testList() {
        Player player = server.addPlayer();
        Command cmd = plugin.getCommand("chaoschickens");
        assertNotNull(cmd);
        assertTrue(command.onCommand(player, cmd, "chaoschickens", new String[]{"list"}));
    }

    @Test
    void testSpawn() {
        Player player = server.addPlayer();
        Command cmd = plugin.getCommand("chaoschickens");
        assertNotNull(cmd);
        assertTrue(command.onCommand(player, cmd, "chaoschickens", new String[]{"spawn", "explosive"}));
    }

    @Test
    void testGive() {
        Player player = server.addPlayer();
        Command cmd = plugin.getCommand("chaoschickens");
        assertNotNull(cmd);
        assertTrue(command.onCommand(player, cmd, "chaoschickens", new String[]{"give", "explosive"}));
    }
}
