package com.chaoschickens.bukkit.trait;

import com.chaoschickens.common.trait.TraitType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

import java.util.Collection;

/**
 * Magnet chicken trait.
 * Periodically pulls nearby item entities toward the chicken,
 * with a chance to steal (remove) them.
 */
public class MagnetTrait extends BukkitTrait {

    private static final double MAGNET_RANGE = 6.0;
    private static final double STEAL_CHANCE = 0.30;
    private static final double PULL_SPEED = 0.4;

    public MagnetTrait() {
        super(TraitType.MAGNET, "Attracts nearby items... and sometimes steals them!", 0.8,
                false, true, 10);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        chicken.setCustomName(ChatColor.DARK_AQUA + "Magnet Chicken");
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;

        Collection<Entity> nearbyEntities = chicken.getNearbyEntities(MAGNET_RANGE, MAGNET_RANGE, MAGNET_RANGE);
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Item) {
                Item item = (Item) entity;
                if (item.isDead()) continue;

                // Pull item toward chicken
                org.bukkit.util.Vector direction = chicken.getLocation().toVector()
                        .subtract(item.getLocation().toVector())
                        .normalize()
                        .multiply(PULL_SPEED);
                item.setVelocity(direction);

                // Chance to steal (remove) the item
                if (getPluginRandom(chicken).nextDouble() < STEAL_CHANCE) {
                    item.remove();
                }
            }
        }
    }

    /**
     * Get a shared Random instance from the plugin, or fallback to a cached one.
     */
    private java.util.Random getPluginRandom(Chicken chicken) {
        try {
            org.bukkit.plugin.Plugin plugin =
                    org.bukkit.Bukkit.getPluginManager().getPlugin("ChaosChickens");
            if (plugin instanceof ChaosChickensBukkit) {
                return ((ChaosChickensBukkit) plugin).getRandom();
            }
        } catch (Exception ignored) {}
        return FALLBACK_RANDOM;
    }

    private static final java.util.Random FALLBACK_RANDOM = new java.util.Random();
}
