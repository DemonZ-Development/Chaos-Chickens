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
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

/**
 * Golden chicken trait.
 * Drops random ores instead of eggs and glows with a golden aura.
 */
public class GoldenTrait extends FabricTrait {

    public GoldenTrait() {
        super(TraitType.GOLDEN, "Drops precious ores instead of eggs!", 0.5,
                false, false, 20);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Component.literal("Golden Chicken").withStyle(ChatFormatting.GOLD));
        chicken.setCustomNameVisible(true);
        chicken.setGlowingTag(true);
    }
}
