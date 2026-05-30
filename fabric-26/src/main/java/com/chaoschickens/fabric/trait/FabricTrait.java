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

import com.chaoschickens.common.trait.AbstractTrait;
import com.chaoschickens.common.trait.TraitType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.player.Player;

public abstract class FabricTrait extends AbstractTrait {

    protected FabricTrait(TraitType type, String description, double defaultWeight,
                          boolean hostile, boolean periodic, int tickInterval) {
        super(type, description, defaultWeight, hostile, periodic, tickInterval);
    }

    public abstract void onApply(Chicken chicken);
    public void onTick(Chicken chicken) { }
    public void onDeath(Chicken chicken, DamageSource source) { }
    public void onPlayerNear(Chicken chicken, Player player) { }
    public double getProximityRange() { return 0; }
}
