package com.chaoschickens.fabric.mixin;

import com.chaoschickens.fabric.ChaosChickensFabric;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.chaoschickens.common.trait.TraitType;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isFireImmune", at = @At("HEAD"), cancellable = true)
    private void chaoschickens$isFireImmune(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Chicken chicken) {
            TraitType type = ChaosChickensFabric.getActiveTrait(chicken).orElse(TraitType.EMPTY);
            if (type == TraitType.FIRE) {
                cir.setReturnValue(true);
            } else if (type == TraitType.BOSS) {
                java.util.List<TraitType> subTraits = com.chaoschickens.fabric.util.ChickenDataUtil.getBossSubTraits(chicken);
                if (subTraits.contains(TraitType.FIRE)) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void chaoschickens$onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Chicken) {
            if (reason == Entity.RemovalReason.CHANGED_DIMENSION) return;
            ChaosChickensFabric.removeActiveChicken(self.getUUID());
        }
    }
}
