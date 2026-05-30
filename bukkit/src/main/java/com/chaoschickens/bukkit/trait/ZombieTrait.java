/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.bukkit.trait;

import com.chaoschickens.common.trait.TraitType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Zombie chicken trait.
 * Hostile chicken that chases nearby players and deals damage on contact.
 */
public class ZombieTrait extends BukkitTrait {

    private static final double CHASE_RANGE = 8.0;
    private static final double DAMAGE_RANGE = 1.5;
    private static final double DAMAGE_AMOUNT = 4.0;
    private static final double CHASE_SPEED = 0.10;

    public ZombieTrait() {
        super(TraitType.ZOMBIE, "A hostile chicken that chases and attacks players!", 0.7,
                true, true, 1);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        chicken.setCustomName(ChatColor.GREEN + "Zombie Chicken");
        chicken.setCustomNameVisible(true);
        if (chicken.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) != null) {
            chicken.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
            chicken.setHealth(20.0);
        }
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;

        // Find nearest player within chase range
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity entity : chicken.getNearbyEntities(CHASE_RANGE, CHASE_RANGE, CHASE_RANGE)) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                double distance = chicken.getLocation().distance(player.getLocation());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestPlayer = player;
                }
            }
        }

        if (nearestPlayer != null) {
            // Chase the player using pathfinding
            boolean navigated = false;
            if (chicken.getLocation().getChunk().isLoaded() && nearestPlayer.getLocation().getChunk().isLoaded()) {
                try {
                    java.lang.reflect.Method getPathfinder = chicken.getClass().getMethod("getPathfinder");
                    Object pathfinder = getPathfinder.invoke(chicken);
                    java.lang.reflect.Method moveTo = pathfinder.getClass().getMethod("moveTo", org.bukkit.entity.LivingEntity.class, double.class);
                    moveTo.invoke(pathfinder, nearestPlayer, 1.25);
                    navigated = true;
                } catch (Exception e) {
                    // Fallback
                }
            }

            if (!navigated && chicken.getNoDamageTicks() < 10) {
                Vector chickenPos = chicken.getLocation().toVector();
                Vector targetPos = nearestPlayer.getLocation().toVector();
                Vector diff = new Vector(targetPos.getX() - chickenPos.getX(), 0.0, targetPos.getZ() - chickenPos.getZ());
                if (diff.lengthSquared() > 0) {
                    Vector direction = diff.normalize().multiply(CHASE_SPEED);
                    double ySpeed = chicken.getVelocity().getY();
                    if (nearestPlayer.getLocation().getY() > chicken.getLocation().getY() + 0.5 && chicken.isOnGround()) {
                        ySpeed = 0.42;
                    }
                    direction.setY(ySpeed);
                    chicken.setVelocity(direction);
                }
            }

            // Damage on contact
            if (nearestDistance <= DAMAGE_RANGE) {
                nearestPlayer.damage(DAMAGE_AMOUNT, chicken);
            }
        }
    }
}
