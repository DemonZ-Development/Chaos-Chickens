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
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
    public void onApply(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Text.literal("Golden Chicken").formatted(Formatting.GOLD));
        chicken.setCustomNameVisible(true);
        chicken.setGlowing(true);
    }

}
