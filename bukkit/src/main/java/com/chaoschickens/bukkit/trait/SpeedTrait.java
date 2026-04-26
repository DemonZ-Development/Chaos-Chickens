package com.chaoschickens.bukkit.trait;

import com.chaoschickens.common.trait.TraitType;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Chicken;

/**
 * Speed chicken trait.
 * Increases movement speed and gives a yellow custom name.
 */
public class SpeedTrait extends BukkitTrait {

    private static final double SPEED_VALUE = 0.35;
    private static final double NORMAL_SPEED = 0.25;

    public SpeedTrait() {
        super(TraitType.SPEED, "A very fast chicken! Zoom zoom!", 1.2,
                false, false, 20);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        // Set generic movement speed attribute
        AttributeInstance speedAttr = chicken.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(SPEED_VALUE);
        }
        chicken.setCustomName(ChatColor.YELLOW + "Speed Chicken");
        chicken.setCustomNameVisible(true);
    }
}
