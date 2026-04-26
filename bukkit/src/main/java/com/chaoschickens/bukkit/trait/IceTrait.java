package com.chaoschickens.bukkit.trait;

import com.chaoschickens.common.trait.TraitType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Chicken;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Ice chicken trait.
 * Freezes water blocks it walks on and drops ice on death.
 * Displays snowflake particles around it.
 * Fix: Uses Location.clone() to avoid mutating the entity's actual location.
 */
public class IceTrait extends BukkitTrait {

    public IceTrait() {
        super(TraitType.ICE, "Freezes water beneath it and drops ice on death!", 0.8,
                false, true, 40);
    }

    @Override
    public boolean modifiesDrops() {
        return true;
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        chicken.setCustomName(ChatColor.AQUA + "Ice Chicken");
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;

        Location chickenLoc = chicken.getLocation(); // Do NOT mutate this

        // Freeze water block the chicken is standing on (use clone to avoid mutation)
        Block blockBelow = chickenLoc.clone().subtract(0, 1, 0).getBlock();
        if (blockBelow.getType() == Material.WATER) {
            blockBelow.setType(Material.ICE);
        }

        // Also check the block at the chicken's feet
        Block blockAt = chickenLoc.getBlock();
        if (blockAt.getType() == Material.WATER) {
            blockAt.setType(Material.ICE);
        }

        // Also freeze water in a 1-block radius around the chicken
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                Block nearby = chickenLoc.clone().add(dx, -1, dz).getBlock();
                if (nearby.getType() == Material.WATER) {
                    nearby.setType(Material.ICE);
                }
            }
        }

        // Display snowflake particles around the chicken (use clone)
        chicken.getWorld().spawnParticle(
                Particle.SNOWFLAKE,
                chickenLoc.clone().add(0, 0.5, 0),
                5,
                0.4, 0.3, 0.4,
                0.01
        );
    }

    @Override
    public void onDeath(Chicken chicken, EntityDeathEvent event) {
        if (chicken == null || event == null) return;

        // Remove default egg drops and add ice instead
        event.getDrops().removeIf(item -> item.getType() == Material.EGG);
        event.getDrops().add(new ItemStack(Material.ICE, 1));
    }
}
