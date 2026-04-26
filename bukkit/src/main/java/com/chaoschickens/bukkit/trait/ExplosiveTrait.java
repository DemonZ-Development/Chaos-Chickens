package com.chaoschickens.bukkit.trait;

import com.chaoschickens.common.trait.TraitType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Chicken;

/**
 * Explosive chicken trait.
 * Creates a small explosion on death without destroying blocks.
 */
public class ExplosiveTrait extends BukkitTrait {

    public ExplosiveTrait() {
        super(TraitType.EXPLOSIVE, "Explodes on death! Careful around these chickens.", 1.0,
                false, false, 20);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        chicken.setCustomName(ChatColor.RED + "Explosive Chicken");
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onDeath(Chicken chicken, org.bukkit.event.entity.EntityDeathEvent event) {
        if (chicken == null) return;
        // Create explosion at death location: power 2.0, no fire, no block damage
        chicken.getWorld().createExplosion(
                chicken.getLocation(),
                2.0f,
                false,  // no fire
                false   // no block damage
        );
    }
}
