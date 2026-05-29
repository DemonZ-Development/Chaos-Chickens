/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.common.trait;

import com.chaoschickens.api.ChaosChickensAPI;

import java.util.Objects;

/**
 * Abstract base implementation of ChaosTrait with common functionality.
 * Platform-specific trait implementations should extend this class.
 */
public abstract class AbstractTrait implements ChaosTrait {

    private final TraitType type;
    private final String description;
    private final double defaultWeight;
    private final boolean hostile;
    private final boolean periodic;
    private final int tickInterval;

    protected AbstractTrait(TraitType type, String description, double defaultWeight,
                            boolean hostile, boolean periodic, int tickInterval) {
        this.type = type;
        this.description = description;
        this.defaultWeight = defaultWeight;
        this.hostile = hostile;
        this.periodic = periodic;
        this.tickInterval = tickInterval;
    }

    @Override
    public TraitType getType() {
        return type;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public double getDefaultWeight() {
        return defaultWeight;
    }

    @Override
    public boolean isHostile() {
        return hostile;
    }

    @Override
    public boolean isPeriodic() {
        return periodic;
    }

    @Override
    public int getTickInterval() {
        return tickInterval;
    }

    /**
     * Register this trait with the global API.
     */
    public void register() {
        ChaosChickensAPI.registerTrait(this, defaultWeight);
    }

    @Override
    public String toString() {
        return String.format("ChaosTrait{type=%s, hostile=%s, periodic=%s}",
                type.getKey(), hostile, periodic);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractTrait that = (AbstractTrait) o;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }
}
