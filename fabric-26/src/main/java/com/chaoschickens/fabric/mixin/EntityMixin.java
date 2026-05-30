package com.chaoschickens.fabric.mixin;

import com.chaoschickens.fabric.ChaosChickensFabric;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void chaoschickens$onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Chicken) {
            if (reason == Entity.RemovalReason.CHANGED_DIMENSION) return;
            ChaosChickensFabric.removeActiveChicken(self.getUUID());
        }
    }
}
