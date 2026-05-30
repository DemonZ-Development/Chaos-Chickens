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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/**
 * Speed chicken trait.
 * Applies a movement speed boost to the chicken and gives it a yellow name.
 */
public class SpeedTrait extends FabricTrait {

    /** Identifier for the movement speed attribute modifier. */
    private static final Identifier SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath("chaoschickens", "speed_boost");

    public SpeedTrait() {
        super(TraitType.SPEED, "A very fast chicken! Zoom zoom!", 1.2,
                false, false, 20);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;

        // Set custom name
        chicken.setCustomName(Component.literal("Speed Chicken").withStyle(ChatFormatting.YELLOW));
        chicken.setCustomNameVisible(true);

        // Apply speed attribute modifier
        var speedAttribute = chicken.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            // Remove existing modifier if present
            AttributeModifier existing = speedAttribute.getModifier(SPEED_MODIFIER_ID);
            if (existing != null) {
                speedAttribute.removeModifier(existing);
            }

            // Set speed modifier to 2.0 (adds 200% to base, making it 3x faster)
            AttributeModifier speedModifier = new AttributeModifier(
                    SPEED_MODIFIER_ID,
                    2.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            speedAttribute.addPermanentModifier(speedModifier);
        }
    }
}
