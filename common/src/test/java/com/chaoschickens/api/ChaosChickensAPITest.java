/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.api;

import com.chaoschickens.common.trait.ChaosTrait;
import com.chaoschickens.common.trait.TraitType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ChaosChickensAPITest {

    @BeforeEach
    void setUp() {
        ChaosChickensAPI.reset();
    }

    @AfterEach
    void tearDown() {
        ChaosChickensAPI.reset();
    }

    @Test
    void testRegisterTrait() {
        ChaosTrait trait = createTestTrait(TraitType.EXPLOSIVE);
        ChaosChickensAPI.registerTrait(trait, 1.0);
        Optional<ChaosTrait> retrieved = ChaosChickensAPI.getTrait(TraitType.EXPLOSIVE);
        assertTrue(retrieved.isPresent());
        assertEquals(trait, retrieved.get());
    }

    @Test
    void testUnregisterTrait() {
        ChaosChickensAPI.registerTrait(createTestTrait(TraitType.SPEED), 1.0);
        assertTrue(ChaosChickensAPI.unregisterTrait(TraitType.SPEED));
        assertFalse(ChaosChickensAPI.getTrait(TraitType.SPEED).isPresent());
    }

    @Test
    void testGetAllTraits() {
        ChaosChickensAPI.registerTrait(createTestTrait(TraitType.FIRE), 1.0);
        ChaosChickensAPI.registerTrait(createTestTrait(TraitType.ICE), 1.0);
        assertEquals(2, ChaosChickensAPI.getAllTraits().size());
    }

    @Test
    void testSetTraitEnabled() {
        ChaosChickensAPI.registerTrait(createTestTrait(TraitType.EXPLOSIVE), 1.0);
        assertTrue(ChaosChickensAPI.isTraitEnabled(TraitType.EXPLOSIVE));
        ChaosChickensAPI.setTraitEnabled(TraitType.EXPLOSIVE, false);
        assertFalse(ChaosChickensAPI.isTraitEnabled(TraitType.EXPLOSIVE));
    }

    @Test
    void testPickRandomTrait() {
        ChaosChickensAPI.registerTrait(createTestTrait(TraitType.EXPLOSIVE), 1.0);
        ChaosChickensAPI.registerTrait(createTestTrait(TraitType.SPEED), 1.0);
        TraitType picked = ChaosChickensAPI.pickRandomTrait(new Random());
        assertTrue(picked == TraitType.EXPLOSIVE || picked == TraitType.SPEED);
    }

    @Test
    void testPickRandomTraitReturnsEmptyWhenNoneEnabled() {
        ChaosChickensAPI.registerTrait(createTestTrait(TraitType.EXPLOSIVE), 1.0);
        ChaosChickensAPI.setTraitEnabled(TraitType.EXPLOSIVE, false);
        assertEquals(TraitType.EMPTY, ChaosChickensAPI.pickRandomTrait(new Random()));
    }

    @Test
    void testInitialize() {
        ChaosChickensAPI.reset();
        ChaosChickensAPI.initialize();
        assertNotNull(ChaosChickensAPI.getAllTraits());
    }

    @Test
    void testReset() {
        ChaosChickensAPI.registerTrait(createTestTrait(TraitType.EXPLOSIVE), 1.0);
        ChaosChickensAPI.reset();
        assertTrue(ChaosChickensAPI.getAllTraits().isEmpty());
    }

    @Test
    void testRegisterTraitThrowsOnNull() {
        assertThrows(IllegalArgumentException.class,
                () -> ChaosChickensAPI.registerTrait(null, 1.0));
    }

    private ChaosTrait createTestTrait(TraitType type) {
        return new ChaosTrait() {
            @Override
            public TraitType getType() { return type; }
            @Override
            public String getDescription() { return "Test trait"; }
        };
    }
}
