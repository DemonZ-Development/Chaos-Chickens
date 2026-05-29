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
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * Speed chicken trait.
 * Applies a movement speed boost to the chicken and gives it a yellow name.
 */
public class SpeedTrait extends FabricTrait {

    /** Identifier for the movement speed attribute modifier. */
    private static final Identifier SPEED_MODIFIER_ID = Identifier.of("chaoschickens", "speed_boost");

    public SpeedTrait() {
        super(TraitType.SPEED, "A very fast chicken! Zoom zoom!", 1.2,
                false, false, 20);
    }

    @Override
    public void onApply(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;

        // Set custom name
        chicken.setCustomName(Text.literal("Speed Chicken").formatted(Formatting.YELLOW));
        chicken.setCustomNameVisible(true);

        // Apply speed attribute modifier
        var speedAttribute = chicken.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speedAttribute != null) {
            // Remove existing modifier if present
            EntityAttributeModifier existing = speedAttribute.getModifier(SPEED_MODIFIER_ID);
            if (existing != null) {
                speedAttribute.removeModifier(existing);
            }

            // Set speed modifier to 2.0 (adds 200% to base, making it 3x faster)
            EntityAttributeModifier speedModifier = new EntityAttributeModifier(
                    SPEED_MODIFIER_ID,
                    2.0,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            speedAttribute.addPersistentModifier(speedModifier);
        }
    }
}
