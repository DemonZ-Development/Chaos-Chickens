/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.fabric.trait;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;

/**
 * Explosive chicken trait.
 * Creates a small explosion when the chicken dies, with no block damage.
 */
public class ExplosiveTrait extends FabricTrait {

    /** Explosion power (2.0 = TNT-like, but we disable block damage). */
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
        Level world = chicken.level();
        boolean damageBlocks = com.chaoschickens.fabric.util.ConfigLoader.getConfig().isExplosiveChickenDamageBlocks();
        // Create explosion that optionally damages blocks
        world.explode(
                chicken,
                chicken.getX(),
                chicken.getY(),
                chicken.getZ(),
                EXPLOSION_POWER,
                damageBlocks ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE
        );
    }
}
