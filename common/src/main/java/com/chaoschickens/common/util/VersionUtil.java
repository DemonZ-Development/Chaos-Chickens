package com.chaoschickens.common.util;

/**
 * Version utility for checking Minecraft version compatibility.
 * Supports version strings in the format "1.MAJOR.MINOR" (e.g., "1.20.4").
 */
public final class VersionUtil {

    private VersionUtil() {
        // Utility class
    }

    /**
     * Compare two Minecraft version strings.
     *
     * @param version1 First version (e.g., "1.20.4")
     * @param version2 Second version (e.g., "1.21.0")
     * @return Negative if v1 < v2, 0 if equal, positive if v1 > v2
     */
    public static int compareVersions(String version1, String version2) {
        String[] parts1 = normalizeVersion(version1).split("\\.");
        String[] parts2 = normalizeVersion(version2).split("\\.");

        int maxLen = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLen; i++) {
            int v1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int v2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (v1 != v2) return Integer.compare(v1, v2);
        }
        return 0;
    }

    /**
     * Check if a version is at least the minimum required version.
     *
     * @param version The version to check
     * @param minimum The minimum required version
     * @return true if version >= minimum
     */
    public static boolean isAtLeast(String version, String minimum) {
        return compareVersions(version, minimum) >= 0;
    }

    /**
     * Normalize a version string by removing prefixes like "v" or "mc".
     *
     * @param version The raw version string
     * @return Normalized version string
     */
    private static String normalizeVersion(String version) {
        if (version == null) return "0";
        return version.replaceAll("^[vVmcMC]+", "");
    }

    /**
     * Parse the major version from a Minecraft version string.
     * E.g., "1.20.4" returns 20.
     *
     * @param version The version string
     * @return The major version number
     */
    public static int getMajorVersion(String version) {
        String[] parts = normalizeVersion(version).split("\\.");
        if (parts.length >= 2) {
            try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Parse the minor version from a Minecraft version string.
     * E.g., "1.20.4" returns 4.
     *
     * @param version The version string
     * @return The minor version number
     */
    public static int getMinorVersion(String version) {
        String[] parts = normalizeVersion(version).split("\\.");
        if (parts.length >= 3) {
            try {
                return Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}
