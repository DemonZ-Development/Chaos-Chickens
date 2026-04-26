package com.chaoschickens.common.trait;

/**
 * Base interface for all chaos chicken traits.
 * Each trait defines unique behavior that modifies how a chicken acts in the world.
 *
 * Platform-specific implementations should extend this interface to add
 * platform-specific methods (e.g., Bukkit's Entity API, Fabric's Mixin support).
 */
public interface ChaosTrait {

    /**
     * Get the type of this trait.
     *
     * @return The TraitType enum value
     */
    TraitType getType();

    /**
     * Get the display name of this trait (used in chat/messages).
     *
     * @return The display name
     */
    default String getDisplayName() {
        return getType().getDisplayName();
    }

    /**
     * Get the description of this trait.
     *
     * @return A short description
     */
    String getDescription();

    /**
     * Get the default spawn weight for this trait.
     * Higher values make the trait more common.
     *
     * @return The default weight (typically 1.0)
     */
    default double getDefaultWeight() {
        return 1.0;
    }

    /**
     * Check if this trait is hostile (damages players).
     *
     * @return true if the trait can harm players
     */
    default boolean isHostile() {
        return false;
    }

    /**
     * Check if this trait modifies drops.
     *
     * @return true if the trait changes what the chicken drops
     */
    default boolean modifiesDrops() {
        return false;
    }

    /**
     * Check if this trait has periodic behavior (runs on a timer).
     *
     * @return true if the trait needs a repeating task/tick handler
     */
    default boolean isPeriodic() {
        return false;
    }

    /**
     * Get the tick interval for periodic traits.
     * Only used if isPeriodic() returns true.
     *
     * @return Tick interval (20 ticks = 1 second)
     */
    default int getTickInterval() {
        return 20; // 1 second default
    }

    /**
     * Check if this trait can stack with other traits on the same chicken.
     *
     * @return true if this trait can coexist with others
     */
    default boolean canStack() {
        return false;
    }

    /**
     * Get the minimum Minecraft version required for this trait.
     *
     * @return Version string, e.g., "1.20.4"
     */
    default String getMinVersion() {
        return "1.20.4";
    }
}
