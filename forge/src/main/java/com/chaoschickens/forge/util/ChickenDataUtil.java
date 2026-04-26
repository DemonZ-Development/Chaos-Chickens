package com.chaoschickens.forge.util;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Chicken;

/**
 * Utility class for reading and writing trait data on chicken entities
 * using Forge NBT tags. Trait data persists across server restarts.
 */
public final class ChickenDataUtil {

    public static final String NBT_KEY = "ChaosChickensTrait";

    private ChickenDataUtil() {}

    public static void setTrait(Chicken chicken, TraitType type) {
        if (chicken == null || type == null) return;
        chicken.getPersistentData().putString(NBT_KEY, type.getKey());
    }

    public static TraitType getTrait(Chicken chicken) {
        if (chicken == null) return TraitType.EMPTY;
        CompoundTag persistentData = chicken.getPersistentData();
        if (!persistentData.contains(NBT_KEY, CompoundTag.TAG_STRING)) return TraitType.EMPTY;
        return TraitType.fromKey(persistentData.getString(NBT_KEY));
    }

    public static boolean hasTrait(Chicken chicken) {
        if (chicken == null) return false;
        return chicken.getPersistentData().contains(NBT_KEY, CompoundTag.TAG_STRING);
    }

    public static void removeTrait(Chicken chicken) {
        if (chicken == null) return;
        chicken.getPersistentData().remove(NBT_KEY);
    }
}
