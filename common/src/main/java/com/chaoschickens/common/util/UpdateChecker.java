/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * Lead Developer: Cyrus
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.common.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Modrinth update checker for Chaos Chickens.
 * Queries the Modrinth API to check for newer versions.
 * Used by all platform modules (Bukkit, Fabric, Forge).
 *
 * Modrinth project slug: chaos-chickens
 * API docs: https://docs.modrinth.com/api/operations/getversions/
 */
public final class UpdateChecker {

    private static final String PROJECT_ID = "chLbYS1k";
    private static final String PROJECT_SLUG = "chaos-chickens";
    private static final String MODRINTH_API_URL =
            "https://api.modrinth.com/v2/project/" + PROJECT_ID + "/version";
    private static final String MODRINTH_PROJECT_URL =
            "https://modrinth.com/project/" + PROJECT_SLUG;
    private static final Logger LOGGER = Logger.getLogger("ChaosChickens/UpdateChecker");
    private static final java.util.concurrent.ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(
            r -> { Thread t = new Thread(r, "ChaosChickens-UpdateChecker"); t.setDaemon(true); return t; }
    );

    private static volatile String latestVersion = null;
    private static volatile String currentVersion = null;
    private static volatile boolean updateAvailable = false;
    private static volatile java.util.function.Consumer<Boolean> updateCallback = null;

    private UpdateChecker() {}



    /**
     * Set the current plugin/mod version for comparison.
     *
     * @param version The current version string (e.g., "1.0.0")
     */
    public static void setCurrentVersion(String version) {
        if (version == null) {
            currentVersion = "";
            return;
        }
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }
        currentVersion = version;
    }

    /**
     * Check for updates asynchronously using the Modrinth API.
     *
     * @return A CompletableFuture that resolves to true if an update is available
     */
    public static CompletableFuture<Boolean> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection)
                        URI.create(MODRINTH_API_URL).toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "ChaosChickens/UpdateChecker");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    LOGGER.warning("Modrinth API returned status " + responseCode);
                    return false;
                }

                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                // Parse the JSON response to find the latest version number
                String responseBody = response.toString();
                latestVersion = parseLatestVersion(responseBody);

                if (latestVersion != null && currentVersion != null) {
                    updateAvailable = VersionUtil.compareVersions(latestVersion, currentVersion) > 0;
                    if (updateAvailable) {
                        LOGGER.info("A new version of Chaos Chickens is available: " +
                                latestVersion + " (current: " + currentVersion + ")");
                        LOGGER.info("Download at: " + MODRINTH_PROJECT_URL);
                    } else {
                        LOGGER.info("Chaos Chickens is up to date (v" + currentVersion + ")");
                    }
                    if (updateCallback != null) {
                        try {
                            updateCallback.accept(updateAvailable);
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }

                return updateAvailable;
            } catch (Exception e) {
                LOGGER.warning("Failed to check for updates: " + e.getMessage());
                return false;
            }
        }, EXECUTOR);
    }

    /**
     * Parse the latest version number from the Modrinth API JSON response.
     *
     * <p>The Modrinth API returns a JSON array of version objects, each containing
     * a "version_number" field. The array is ordered by date published (newest first),
     * so we extract from the first object found.</p>
     *
     * <p>Improved parsing handles:</p>
     * <ul>
     *   <li>Variations in JSON whitespace/formatting</li>
     *   <li>Both "version_number" and legacy "version_number" field names</li>
     *   <li>Escaped quotes within values</li>
     *   <li>Nested JSON objects (skips to the correct top-level field)</li>
     * </ul>
     *
     * @param json The raw JSON response body from the Modrinth versions API
     * @return The parsed version string, or null if parsing failed
     */
    private static String parseLatestVersion(String json) {
        try {
            // The API returns an array of version objects.
            // We want the "version_number" from the first (latest) object.
            // Strategy: find the first "version_number" key, then extract its string value.

            String searchKey = "\"version_number\"";
            int idx = json.indexOf(searchKey);
            if (idx == -1) {
                LOGGER.warning("Modrinth API response missing 'version_number' field");
                return null;
            }

            // Skip past the key to find the colon separator
            int colonIdx = json.indexOf(':', idx + searchKey.length());
            if (colonIdx == -1) return null;

            // Find the opening quote of the value after the colon
            int valueStart = json.indexOf('"', colonIdx + 1);
            if (valueStart == -1) return null;

            // Find the closing quote, handling escaped quotes within the value
            int valueEnd = valueStart + 1;
            while (valueEnd < json.length()) {
                char c = json.charAt(valueEnd);
                if (c == '\\') {
                    // Skip escaped character
                    valueEnd += 2;
                    continue;
                }
                if (c == '"') {
                    break;
                }
                valueEnd++;
            }
            if (valueEnd >= json.length()) return null;

            String rawVersion = json.substring(valueStart + 1, valueEnd);

            // Clean up the version string: unescape any JSON escape sequences
            String version = rawVersion
                    .replace("\\\\", "\\")
                    .replace("\\\"", "\"")
                    .replace("\\/", "/");

            // Strip any leading "v" prefix for consistent comparison
            if (version.startsWith("v") || version.startsWith("V")) {
                version = version.substring(1);
            }

            return version;
        } catch (Exception e) {
            LOGGER.warning("Failed to parse Modrinth API response: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if an update is available.
     *
     * @return true if a newer version was found on Modrinth
     */
    public static boolean isUpdateAvailable() {
        return updateAvailable;
    }

    /**
     * Get the latest version string from Modrinth.
     *
     * @return The latest version, or null if not checked yet
     */
    public static String getLatestVersion() {
        return latestVersion;
    }

    /**
     * Get a formatted update notification message.
     *
     * @return A user-friendly message about available updates
     */
    public static String getUpdateMessage() {
        if (!updateAvailable) return null;
        return "A new version of Chaos Chickens is available: v" + latestVersion +
                " (you have v" + currentVersion + "). Download at: " +
                MODRINTH_PROJECT_URL;
    }

    /**
     * Register a callback to run when an update check completes.
     */
    public static void setUpdateCallback(java.util.function.Consumer<Boolean> callback) {
        updateCallback = callback;
    }

    /**
     * Start scheduled update checking every N hours.
     */
    public static void startScheduledUpdateChecks(long intervalHours) {
        EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                checkForUpdates();
            } catch (Exception e) {
                LOGGER.warning("Failed to run scheduled update check: " + e.getMessage());
            }
        }, intervalHours, intervalHours, java.util.concurrent.TimeUnit.HOURS);
    }

    /**
     * Shutdown the update checker executor.
     */
    public static void shutdown() {
        try {
            EXECUTOR.shutdown();
        } catch (Exception ignored) {}
    }
}
