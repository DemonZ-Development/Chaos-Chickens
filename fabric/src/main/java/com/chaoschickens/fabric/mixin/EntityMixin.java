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

import com.chaoschickens.fabric.ChaosChickensFabric;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.ChickenEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into Entity to clean up active chaos chicken tracking when they are removed.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void chaoschickens$onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof ChickenEntity) {
            // Don't remove from tracking on dimension travel — chicken still exists
            if (reason == Entity.RemovalReason.CHANGED_DIMENSION) return;
            ChaosChickensFabric.removeActiveChicken(self.getUuid());
        }
    }
}
