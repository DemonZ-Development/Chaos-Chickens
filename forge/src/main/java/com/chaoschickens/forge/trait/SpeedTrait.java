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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Speed chicken trait.
 * Applies a movement speed boost and gives the chicken a yellow name.
 */
public class SpeedTrait extends ForgeTrait {

    private static final ResourceLocation SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("chaoschickens", "speed_boost");
    public SpeedTrait() {
        super(TraitType.SPEED, "A very fast chicken! Zoom zoom!", 1.2,
                false, false, 20);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;

        chicken.setCustomName(Component.literal("Speed Chicken").withStyle(ChatFormatting.YELLOW));
        chicken.setCustomNameVisible(true);

        // Apply speed attribute modifier
        var speedAttribute = chicken.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.removeModifier(SPEED_MODIFIER_ID);
            AttributeModifier speedModifier = new AttributeModifier(
                    SPEED_MODIFIER_ID,
                    2.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            speedAttribute.addPermanentModifier(speedModifier);
        }
    }

    @Override
    public void onDeath(Chicken chicken, DamageSource source) {
        removeSpeedModifier(chicken);
    }

    public static void removeSpeedModifier(Chicken chicken) {
        if (chicken == null) return;
        var speedAttribute = chicken.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.removeModifier(SPEED_MODIFIER_ID);
        }
    }
}
