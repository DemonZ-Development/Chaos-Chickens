package com.chaoschickens.fabric.util;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.nbt.NbtCompound;

/**
 * Utility class for reading and writing trait data on chicken entities.
 * Bug #1 fix: setTrait() now directly stores the trait in a transient field
 * that the mixin picks up during the next NBT write cycle.
 * The mixin's writeCustomDataFromNbt inject will persist the trait.
 */
public final class ChickenDataUtil {

    public static final String NBT_KEY = "ChaosChickensTrait";

    /** Transient storage for traits that haven't been persisted yet */
    private static final java.util.Map<java.util.UUID, TraitType> pendingTraits =
            new java.util.concurrent.ConcurrentHashMap<>();

    private ChickenDataUtil() {}

    /**
     * Set the trait type on a chicken entity.
     * Bug #1 fix: Store in pending map. The ChickenEntityMixin's
     * writeCustomDataFromNbt will persist it on the next save cycle.
     */
    public static void setTrait(ChickenEntity chicken, TraitType type) {
        if (chicken == null || type == null) return;
        pendingTraits.put(chicken.getUuid(), type);
        // Force NBT write to trigger mixin persistence
        NbtCompound forcedWrite = new NbtCompound();
        chicken.writeCustomDataToNbt(forcedWrite);
    }

    public static TraitType getTrait(ChickenEntity chicken) {
        if (chicken == null) return TraitType.EMPTY;

        // Check pending traits first
        TraitType pending = pendingTraits.get(chicken.getUuid());
        if (pending != null) return pending;

        NbtCompound nbt = new NbtCompound();
        chicken.writeCustomDataToNbt(nbt);
        if (!nbt.contains(NBT_KEY, NbtCompound.STRING_TYPE)) return TraitType.EMPTY;
        String key = nbt.getString(NBT_KEY);
        if (key == null || key.isEmpty()) return TraitType.EMPTY;
        return TraitType.fromKey(key);
    }

    public static boolean hasTrait(ChickenEntity chicken) {
        if (chicken == null) return false;
        if (pendingTraits.containsKey(chicken.getUuid())) return true;
        NbtCompound nbt = new NbtCompound();
        chicken.writeCustomDataToNbt(nbt);
        if (!nbt.contains(NBT_KEY, NbtCompound.STRING_TYPE)) return false;
        String key = nbt.getString(NBT_KEY);
        return key != null && !key.isEmpty();
    }

    public static void removeTrait(ChickenEntity chicken) {
        if (chicken == null) return;
        pendingTraits.remove(chicken.getUuid());
        // Remove from active tracking so the mixin won't re-write the trait to NBT
        ChaosChickensFabric.removeActiveChicken(chicken.getUuid());
    }

    /**
     * Get and remove a pending trait (used by mixin to flush to NBT).
     */
    public static TraitType getAndClearPending(java.util.UUID uuid) {
        return pendingTraits.remove(uuid);
    }

    /**
     * Check if there's a pending trait for a chicken UUID.
     */
    public static boolean hasPendingTrait(java.util.UUID uuid) {
        return pendingTraits.containsKey(uuid);
    }
}
