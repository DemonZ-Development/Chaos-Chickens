/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionUtilTest {

    @Test
    void testCompareVersionsEqual() {
        assertEquals(0, VersionUtil.compareVersions("1.20.4", "1.20.4"));
    }

    @Test
    void testCompareVersionsLess() {
        assertTrue(VersionUtil.compareVersions("1.20.4", "1.21.0") < 0);
    }

    @Test
    void testCompareVersionsGreater() {
        assertTrue(VersionUtil.compareVersions("1.21.0", "1.20.4") > 0);
    }

    @Test
    void testCompareVersionsSameMajorDifferentMinor() {
        assertTrue(VersionUtil.compareVersions("1.20.4", "1.20.5") < 0);
    }

    @Test
    void testCompareVersionsDifferentMajor() {
        assertTrue(VersionUtil.compareVersions("1.20.0", "1.21.0") < 0);
    }

    @Test
    void testIsAtLeastTrue() {
        assertTrue(VersionUtil.isAtLeast("1.21.0", "1.20.4"));
    }

    @Test
    void testIsAtLeastFalse() {
        assertFalse(VersionUtil.isAtLeast("1.20.4", "1.21.0"));
    }

    @Test
    void testIsAtLeastEqual() {
        assertTrue(VersionUtil.isAtLeast("1.20.4", "1.20.4"));
    }

    @Test
    void testGetMajorVersion() {
        assertEquals(20, VersionUtil.getMajorVersion("1.20.4"));
    }

    @Test
    void testGetMinorVersion() {
        assertEquals(4, VersionUtil.getMinorVersion("1.20.4"));
    }

    @Test
    void testGetMajorVersionInvalid() {
        assertEquals(0, VersionUtil.getMajorVersion("invalid"));
    }

    @Test
    void testGetMinorVersionInvalid() {
        assertEquals(0, VersionUtil.getMinorVersion("1.20"));
    }

    @Test
    void testGetMajorVersionVPrefix() {
        assertEquals(20, VersionUtil.getMajorVersion("v1.20.4"));
    }

    @Test
    void testGetMinorVersionMCPrefix() {
        assertEquals(4, VersionUtil.getMinorVersion("mc1.20.4"));
    }
}
