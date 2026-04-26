package com.chaoschickens.bukkit.trait;

import com.chaoschickens.bukkit.ChaosChickensBukkit;
import com.chaoschickens.common.trait.TraitType;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.EntityEffect;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Sheep;

/**
 * Disco chicken trait.
 * Plays random note block sounds periodically and changes nearby sheep colors.
 * Bug #8 fix: Removed shared localTickCounter that caused cross-chicken state leakage.
 * Now uses chicken.getTicksLived() for sheep dye timing.
 * Bug #9 fix: Uses shared Random from plugin instance.
 */
public class DiscoTrait extends BukkitTrait {

    public DiscoTrait() {
        super(TraitType.DISCO, "Party time! Plays music and dyes nearby sheep!", 1.0,
                false, true, 15);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        chicken.setCustomName(ChatColor.LIGHT_PURPLE + "Disco Chicken");
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;

        // Get shared Random from plugin
        // Fallback to local Random if plugin not available
        java.util.Random rand = getPluginRandom(chicken);

        // Play random note block sound
        float pitch = 0.5f + rand.nextFloat() * 1.5f;
        chicken.getWorld().playSound(
                chicken.getLocation(),
                org.bukkit.Sound.BLOCK_NOTE_BLOCK_HARP,
                0.7f,
                pitch
        );

        // Change color of nearby sheep every other onTick call using chicken's ticks lived
        if (chicken.getTicksLived() % 2 == 0) {
            for (Entity entity : chicken.getNearbyEntities(8, 8, 8)) {
                if (entity instanceof Sheep) {
                    Sheep sheep = (Sheep) entity;
                    DyeColor[] colors = DyeColor.values();
                    DyeColor randomColor = colors[rand.nextInt(colors.length)];
                    sheep.setColor(randomColor);
                    sheep.playEffect(EntityEffect.SHEEP_EAT_GRASS);
                }
            }
        }
    }

    /**
     * Get a shared Random instance from the plugin, or fallback to a local one.
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
