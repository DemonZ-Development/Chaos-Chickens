/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.common.trait;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TraitTypeTest {

    @Test
    void testFromKeyExplosive() {
        assertEquals(TraitType.EXPLOSIVE, TraitType.fromKey("explosive"));
    }

    @Test
    void testFromKeySpeed() {
        assertEquals(TraitType.SPEED, TraitType.fromKey("speed"));
    }

    @Test
    void testFromKeyFire() {
        assertEquals(TraitType.FIRE, TraitType.fromKey("fire"));
    }

    @Test
    void testFromKeyMagnet() {
        assertEquals(TraitType.MAGNET, TraitType.fromKey("magnet"));
    }

    @Test
    void testFromKeyGolden() {
        assertEquals(TraitType.GOLDEN, TraitType.fromKey("golden"));
    }

    @Test
    void testFromKeyDisco() {
        assertEquals(TraitType.DISCO, TraitType.fromKey("disco"));
    }

    @Test
    void testFromKeyZombie() {
        assertEquals(TraitType.ZOMBIE, TraitType.fromKey("zombie"));
    }

    @Test
    void testFromKeyTeleport() {
        assertEquals(TraitType.TELEPORT, TraitType.fromKey("teleport"));
    }

    @Test
    void testFromKeyIce() {
        assertEquals(TraitType.ICE, TraitType.fromKey("ice"));
    }

    @Test
    void testFromKeyCursed() {
        assertEquals(TraitType.CURSED, TraitType.fromKey("cursed"));
    }

    @Test
    void testFromKeyBoss() {
        assertEquals(TraitType.BOSS, TraitType.fromKey("boss"));
    }

    @Test
    void testFromKeyReturnsEmptyForUnknown() {
        assertEquals(TraitType.EMPTY, TraitType.fromKey("unknown"));
    }

    @Test
    void testFromKeyNullReturnsEmpty() {
        assertEquals(TraitType.EMPTY, TraitType.fromKey(null));
    }

    @Test
    void testFromKeyCaseInsensitive() {
        assertEquals(TraitType.EXPLOSIVE, TraitType.fromKey("EXPLOSIVE"));
        assertEquals(TraitType.EXPLOSIVE, TraitType.fromKey("Explosive"));
    }

    @Test
    void testEmptyKey() {
        assertEquals(TraitType.EMPTY, TraitType.fromKey("empty"));
    }
}
