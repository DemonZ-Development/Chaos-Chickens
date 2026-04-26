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
    private static final double DAMAGE_AMOUNT = 1.0;
    private static final double CHASE_SPEED = 0.25;

    public ZombieTrait() {
        super(TraitType.ZOMBIE, "A hostile chicken that chases and attacks players!", 0.7,
                true, true, 20);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        chicken.setCustomName(ChatColor.DARK_RED + "Zombie Chicken");
        chicken.setCustomNameVisible(true);
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
            // Chase: set velocity toward the player
            Vector direction = nearestPlayer.getLocation().toVector()
                    .subtract(chicken.getLocation().toVector())
                    .normalize()
                    .multiply(CHASE_SPEED);
            direction.setY(0); // Keep it on the ground
            chicken.setVelocity(direction);

            // Damage on contact
            if (nearestDistance <= DAMAGE_RANGE) {
                nearestPlayer.damage(DAMAGE_AMOUNT, chicken);
            }
        }
    }
}
