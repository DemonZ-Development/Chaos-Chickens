package com.chaoschickens.bukkit.trait;

import com.chaoschickens.common.trait.TraitType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.Particle;

import java.util.Random;

public class BossTrait extends BukkitTrait {
    
    public BossTrait() {
        super(TraitType.BOSS, "A powerful chicken with multiple chaos traits combined!", 0.02,
                true, true, 100);
    }
    
    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        chicken.setCustomName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "BOSS Chicken");
        chicken.setCustomNameVisible(true);
        // Boss chickens have 3x health
        chicken.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(12.0);
        chicken.setHealth(12.0);
    }
    
    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        // Boss aura particles
        chicken.getWorld().spawnParticle(Particle.DRAGON_BREATH, 
            chicken.getLocation().clone().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.02);
    }
    
    @Override
    public void onDeath(Chicken chicken, EntityDeathEvent event) {
        if (chicken == null) return;
        // Boss explosion effect (visual only, no block damage)
        chicken.getWorld().spawnParticle(Particle.EXPLOSION_HUGE,
            chicken.getLocation(), 3, 0.5, 0.5, 0.5, 0);
        chicken.getWorld().spawnParticle(Particle.DRAGON_BREATH,
            chicken.getLocation(), 30, 1, 1, 1, 0.05);
    }
    
    @Override
    public double getProximityRange() {
        return 16.0;
    }
    
    @Override
    public void onPlayerNear(Chicken chicken, Player player) {
        // Boss chickens have a menacing aura that gives players weakness
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.WEAKNESS, 40, 0));
    }
}
