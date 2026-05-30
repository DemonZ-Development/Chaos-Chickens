package com.chaoschickens.fabric.mixin;

import com.chaoschickens.common.trait.TraitType;
import com.chaoschickens.fabric.ChaosChickensFabric;
import com.chaoschickens.fabric.util.ChickenDataUtil;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chicken.class)
public abstract class ChickenMixin {

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void chaoschickens$readTrait(CompoundTag nbt, CallbackInfo ci) {
        Chicken self = (Chicken) (Object) this;
        ChaosChickensFabric.addCheckedChicken(self.getUUID());
        if (nbt.contains("BossSubTraits")) {
            String subTraitsStr = nbt.getString("BossSubTraits").orElse("");
            java.util.List<TraitType> list = new java.util.ArrayList<>();
            for (String s : subTraitsStr.split(",")) {
                TraitType t = TraitType.fromKey(s.trim());
                if (t != TraitType.EMPTY && t != TraitType.BOSS) { list.add(t); }
            }
            ChickenDataUtil.setBossSubTraits(self, list);
        }
        if (nbt.contains(ChickenDataUtil.NBT_KEY)) {
            String traitKey = nbt.getString(ChickenDataUtil.NBT_KEY).orElse("");
            TraitType type = TraitType.fromKey(traitKey);
            if (type != TraitType.EMPTY) {
                ChaosChickensFabric.addActiveChicken(self.getUUID(), type);
                com.chaoschickens.fabric.trait.FabricTrait trait = ChaosChickensFabric.getTraitInstance(type);
                if (trait != null) { trait.onApply(self); }
            }
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void chaoschickens$writeTrait(CompoundTag nbt, CallbackInfo ci) {
        Chicken self = (Chicken) (Object) this;
        nbt.putBoolean("ChaosChickensChecked", true);
        java.util.List<TraitType> subTraits = ChickenDataUtil.getBossSubTraits(self);
        if (!subTraits.isEmpty()) {
            java.util.List<String> keys = new java.util.ArrayList<>();
            for (TraitType t : subTraits) { keys.add(t.getKey()); }
            nbt.putString("BossSubTraits", String.join(",", keys));
        }
        if (ChickenDataUtil.hasPendingTrait(self.getUUID())) {
            TraitType pending = ChickenDataUtil.getAndClearPending(self.getUUID());
            nbt.putString(ChickenDataUtil.NBT_KEY, pending.getKey());
            return;
        }
        java.util.Optional<TraitType> traitType = ChaosChickensFabric.getActiveTrait(self);
        traitType.ifPresent(type -> nbt.putString(ChickenDataUtil.NBT_KEY, type.getKey()));
    }
}
