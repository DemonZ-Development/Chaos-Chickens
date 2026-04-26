package com.chaoschickens.bukkit.trait;

import com.chaoschickens.common.trait.TraitType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Chicken;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Golden chicken trait.
 * Drops random ores instead of normal drops on death.
 * Has a glowing effect and a gold custom name.
 */
public class GoldenTrait extends BukkitTrait {

    private static final double GOLD_NUGGET_CHANCE = 0.40;
    private static final double GOLD_INGOT_CHANCE = 0.25;
    private static final double IRON_INGOT_CHANCE = 0.20;
    private static final double EMERALD_CHANCE = 0.10;
    // Diamond = remaining = 0.05

    private final Random random = new Random();

    public GoldenTrait() {
        super(TraitType.GOLDEN, "Drops precious ores instead of eggs!", 0.5,
                false, false, 20);
    }

    @Override
    public boolean modifiesDrops() {
        return true;
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        chicken.setCustomName(ChatColor.GOLD + "Golden Chicken");
        chicken.setCustomNameVisible(true);
        // Add glowing effect
        chicken.setGlowing(true);
    }

    @Override
    public void onDeath(Chicken chicken, EntityDeathEvent event) {
        if (chicken == null || event == null) return;

        // Remove default drops (raw chicken, feathers, egg)
        event.getDrops().clear();

        // Drop a random ore item based on weighted chance
        Material dropMaterial = pickRandomOre();
        event.getDrops().add(new ItemStack(dropMaterial, 1));
    }

    private Material pickRandomOre() {
        double roll = random.nextDouble();
        if (roll < GOLD_NUGGET_CHANCE) {
            return Material.GOLD_NUGGET;
        } else if (roll < GOLD_NUGGET_CHANCE + GOLD_INGOT_CHANCE) {
            return Material.GOLD_INGOT;
        } else if (roll < GOLD_NUGGET_CHANCE + GOLD_INGOT_CHANCE + IRON_INGOT_CHANCE) {
            return Material.IRON_INGOT;
        } else if (roll < GOLD_NUGGET_CHANCE + GOLD_INGOT_CHANCE + IRON_INGOT_CHANCE + EMERALD_CHANCE) {
            return Material.EMERALD;
        } else {
            return Material.DIAMOND;
        }
    }
}
