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
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

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
    public void onApply(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Text.literal("Magnet Chicken").formatted(Formatting.DARK_AQUA));
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.getWorld() instanceof ServerWorld serverWorld)) return;

        // Find all item entities within range
        List<ItemEntity> nearbyItems = serverWorld.getEntitiesByClass(
                ItemEntity.class,
                chicken.getBoundingBox().expand(ATTRACTION_RANGE),
                item -> item.isAlive() && !item.cannotPickup()
        );

        Vec3d chickenPos = chicken.getPos();

        for (ItemEntity item : nearbyItems) {
            // Pull item toward chicken
            Vec3d itemPos = item.getPos();
            Vec3d velocity = chickenPos.subtract(itemPos);
            if (velocity.lengthSquared() < 0.01) continue;
            Vec3d direction = velocity.normalize().multiply(PULL_STRENGTH);
            item.setVelocity(item.getVelocity().add(direction));
            item.velocityModified = true;

            // Chance to steal (remove) the item
            if (chicken.getRandom().nextDouble() < STEAL_CHANCE) {
                item.discard();
            }
        }
    }
}
