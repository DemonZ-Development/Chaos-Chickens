/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.fabric.mixin;

import com.chaoschickens.common.trait.TraitType;
import com.chaoschickens.fabric.ChaosChickensFabric;
import com.chaoschickens.fabric.util.ChickenDataUtil;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into ChickenEntity to handle trait persistence via NBT
 * and trait-specific tick behavior.
 */
@Mixin(ChickenEntity.class)
public abstract class ChickenEntityMixin {

    /**
     * Inject at the tail of readCustomDataFromNbt to load trait data.
     * When a chicken is loaded from disk (chunk load, server restart),
     * we restore its trait and add it to the active tracking map.
     */
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void chaoschickens$readTrait(NbtCompound nbt, CallbackInfo ci) {
        ChickenEntity self = (ChickenEntity) (Object) this;
        ChaosChickensFabric.addCheckedChicken(self.getUuid());
        if (nbt.contains("BossSubTraits", NbtCompound.STRING_TYPE)) {
            String subTraitsStr = nbt.getString("BossSubTraits");
            java.util.List<TraitType> list = new java.util.ArrayList<>();
            for (String s : subTraitsStr.split(",")) {
                TraitType t = TraitType.fromKey(s.trim());
                if (t != TraitType.EMPTY && t != TraitType.BOSS) {
                    list.add(t);
                }
            }
            ChickenDataUtil.setBossSubTraits(self, list);
        }
        if (nbt.contains(ChickenDataUtil.NBT_KEY, NbtCompound.STRING_TYPE)) {
            String traitKey = nbt.getString(ChickenDataUtil.NBT_KEY);
            TraitType type = TraitType.fromKey(traitKey);
            if (type != TraitType.EMPTY) {
                ChaosChickensFabric.addActiveChicken(self.getUuid(), type);

                // Re-apply trait effects (custom name, attributes, etc.)
                com.chaoschickens.fabric.trait.FabricTrait trait =
                        ChaosChickensFabric.getTraitInstance(type);
                if (trait != null) {
                    trait.onApply(self);
                }
            }
        }
    }

    /**
     * Inject at the tail of writeCustomDataToNbt to save trait data.
     * Ensures trait data persists across server restarts and chunk unloads.
     */
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void chaoschickens$writeTrait(NbtCompound nbt, CallbackInfo ci) {
        ChickenEntity self = (ChickenEntity) (Object) this;
        nbt.putBoolean("ChaosChickensChecked", true);
        java.util.List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(self);
        if (!subTraits.isEmpty()) {
            java.util.List<String> keys = new java.util.ArrayList<>();
            for (TraitType t : subTraits) {
                keys.add(t.getKey());
            }
            nbt.putString("BossSubTraits", String.join(",", keys));
        }
        // Check pending traits first (from setTrait)
        if (ChickenDataUtil.hasPendingTrait(self.getUuid())) {
            TraitType pending = ChickenDataUtil.getAndClearPending(self.getUuid());
            nbt.putString(ChickenDataUtil.NBT_KEY, pending.getKey());
            return;
        }
        // Otherwise check active tracking
        java.util.Optional<TraitType> traitType = ChaosChickensFabric.getActiveTrait(self);
        traitType.ifPresent(type -> nbt.putString(ChickenDataUtil.NBT_KEY, type.getKey()));
    }
}
