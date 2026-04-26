package com.chaoschickens.bukkit.util;

import com.chaoschickens.common.trait.TraitType;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Chicken;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Utility class for reading and writing trait data on chicken entities
 * using Bukkit's PersistentDataContainer API.
 */
public final class ChickenDataUtil {

    private static final String KEY_NAMESPACE = "chaoschickens";
    private static final String KEY_NAME = "trait";

    private ChickenDataUtil() {
        // Utility class
    }

    /**
     * Get the NamespacedKey used to store trait data.
     *
     * @param plugin The plugin instance
     * @return The NamespacedKey for trait storage
     */
    public static NamespacedKey getTraitKey(JavaPlugin plugin) {
        return new NamespacedKey(plugin, KEY_NAME);
    }

    /**
     * Set the trait type on a chicken entity via PersistentDataContainer.
     *
     * @param plugin  The plugin instance
     * @param chicken The chicken entity
     * @param type    The trait type to assign
     */
    public static void setTrait(JavaPlugin plugin, Chicken chicken, TraitType type) {
        if (chicken == null || type == null) return;
        PersistentDataContainer pdc = chicken.getPersistentDataContainer();
        pdc.set(getTraitKey(plugin), PersistentDataType.STRING, type.getKey());
    }

    /**
     * Get the trait type from a chicken entity's PersistentDataContainer.
     *
     * @param plugin  The plugin instance
     * @param chicken The chicken entity
     * @return The TraitType, or EMPTY if no trait is stored
     */
    public static TraitType getTrait(JavaPlugin plugin, Chicken chicken) {
        if (chicken == null) return TraitType.EMPTY;
        PersistentDataContainer pdc = chicken.getPersistentDataContainer();
        NamespacedKey key = getTraitKey(plugin);
        if (!pdc.has(key, PersistentDataType.STRING)) {
            return TraitType.EMPTY;
        }
        String value = pdc.get(key, PersistentDataType.STRING);
        return TraitType.fromKey(value);
    }

    /**
     * Check if a chicken entity has a chaos trait stored.
     *
     * @param plugin  The plugin instance
     * @param chicken The chicken entity
     * @return true if the chicken has a non-EMPTY trait
     */
    public static boolean hasTrait(JavaPlugin plugin, Chicken chicken) {
        if (chicken == null) return false;
        PersistentDataContainer pdc = chicken.getPersistentDataContainer();
        return pdc.has(getTraitKey(plugin), PersistentDataType.STRING);
    }

    /**
     * Remove the trait data from a chicken entity.
     *
     * @param plugin  The plugin instance
     * @param chicken The chicken entity
     */
    public static void removeTrait(JavaPlugin plugin, Chicken chicken) {
        if (chicken == null) return;
        PersistentDataContainer pdc = chicken.getPersistentDataContainer();
        pdc.remove(getTraitKey(plugin));
    }
}
