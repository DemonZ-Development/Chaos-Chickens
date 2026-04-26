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
        if (nbt.contains(ChickenDataUtil.NBT_KEY, NbtCompound.STRING_TYPE)) {
            String traitKey = nbt.getString(ChickenDataUtil.NBT_KEY);
            TraitType type = TraitType.fromKey(traitKey);
            if (type != TraitType.EMPTY) {
                ChickenEntity self = (ChickenEntity) (Object) this;
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

    /**
     * Inject at the head of onDeath to handle trait-specific death behavior.
     * This ensures effects like explosions happen before the entity is fully removed.
     */
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void chaoschickens$onDeath(CallbackInfo ci) {
        ChickenEntity self = (ChickenEntity) (Object) this;
        java.util.Optional<TraitType> traitType = ChaosChickensFabric.getActiveTrait(self);
        traitType.ifPresent(type -> {
            com.chaoschickens.fabric.trait.FabricTrait trait =
                    ChaosChickensFabric.getTraitInstance(type);
            if (trait != null) {
                trait.onDeath(self, self.getRecentDamageSource());
            }
            ChaosChickensFabric.removeActiveChicken(self.getUuid());
        });
    }

    /**
     * Inject at the head of remove to clean up tracking when entity is removed.
     */
    @Inject(method = "remove", at = @At("HEAD"))
    private void chaoschickens$onRemove(CallbackInfo ci) {
        ChickenEntity self = (ChickenEntity) (Object) this;
        ChaosChickensFabric.removeActiveChicken(self.getUuid());
    }
}
