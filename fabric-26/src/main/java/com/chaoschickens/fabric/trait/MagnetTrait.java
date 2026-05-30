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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Magnet chicken trait.
 * Pulls nearby item entities toward the chicken and has a chance to steal them.
 */
public class MagnetTrait extends FabricTrait {

    /** Range in blocks to attract items. */
    private static final double ATTRACTION_RANGE = 6.0;

    /** Chance (0-1) that an attracted item is stolen (removed). */
    private static final double STEAL_CHANCE = 0.30;

    /** Pull strength multiplier. */
    private static final double PULL_STRENGTH = 0.15;

    public MagnetTrait() {
        super(TraitType.MAGNET, "Steals your items! Keep your loot away!", 0.8,
                false, true, 10);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Component.literal("Magnet Chicken").withStyle(ChatFormatting.DARK_AQUA));
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.level() instanceof ServerLevel serverWorld)) return;

        // Find all item entities within range
        List<ItemEntity> nearbyItems = serverWorld.getEntitiesOfClass(
                ItemEntity.class,
                chicken.getBoundingBox().inflate(ATTRACTION_RANGE),
                item -> item.isAlive() && !item.hasPickUpDelay()
        );

        Vec3 chickenPos = chicken.position();

        for (ItemEntity item : nearbyItems) {
            // Pull item toward chicken
            Vec3 itemPos = item.position();
            Vec3 velocity = chickenPos.subtract(itemPos);
            if (velocity.lengthSqr() < 0.01) continue;
            Vec3 direction = velocity.normalize().scale(PULL_STRENGTH);
            item.setDeltaMovement(item.getDeltaMovement().add(direction));
            item.hurtMarked = true;

            // Chance to steal (remove) the item
            if (chicken.getRandom().nextDouble() < STEAL_CHANCE) {
                item.discard();
            }
        }
    }
}
