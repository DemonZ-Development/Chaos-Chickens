/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.forge.trait;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.Chicken;

/**
 * Explosive chicken trait.
 * Creates a small explosion when the chicken dies, with no block damage.
 */
public class ExplosiveTrait extends ForgeTrait {

    private static final float EXPLOSION_POWER = 2.0f;

    public ExplosiveTrait() {
        super(TraitType.EXPLOSIVE, "Explodes when killed! Watch out!", 1.0,
                true, false, 20);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Component.literal("Explosive Chicken").withStyle(ChatFormatting.RED));
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onDeath(Chicken chicken, DamageSource source) {
        if (chicken == null) return;
        if (chicken.level() instanceof ServerLevel serverLevel) {
            boolean damageBlocks = com.chaoschickens.forge.util.ConfigLoader.getConfig().isExplosiveChickenDamageBlocks();
            // Create explosion with optional block damage
            serverLevel.explode(null, chicken.getX(), chicken.getY(), chicken.getZ(),
                    EXPLOSION_POWER, false, damageBlocks ? ServerLevel.ExplosionInteraction.TNT : ServerLevel.ExplosionInteraction.NONE);
        }
    }
}
