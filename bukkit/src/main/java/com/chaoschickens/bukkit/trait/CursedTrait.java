package com.chaoschickens.bukkit.trait;

import com.chaoschickens.common.trait.TraitType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

/**
 * Cursed chicken trait.
 * Applies random bad effects to nearby players.
 */
public class CursedTrait extends BukkitTrait {

    private static final double PROXIMITY_RANGE = 6.0;
    private static final int EFFECT_DURATION = 100; // 5 seconds (in ticks)

    private final Random random = new Random();

    /**
     * Possible cursed effects with their durations.
     */
    private enum CursedEffect {
        POISON(PotionEffectType.POISON, 200),       // 10 seconds
        SLOW(PotionEffectType.SLOW, 200),           // 10 seconds
        WEAKNESS(PotionEffectType.WEAKNESS, 200),   // 10 seconds
        BLINDNESS(PotionEffectType.BLINDNESS, 100), // 5 seconds
        WITHER(PotionEffectType.WITHER, 100);       // 5 seconds

        final PotionEffectType effectType;
        final int duration;

        CursedEffect(PotionEffectType effectType, int duration) {
            this.effectType = effectType;
            this.duration = duration;
        }
    }

    public CursedTrait() {
        super(TraitType.CURSED, "Curses nearby players with random bad effects!", 0.7,
                true, true, 40);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        chicken.setCustomName(ChatColor.DARK_PURPLE + "Cursed Chicken");
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onPlayerNear(Chicken chicken, Player player) {
        if (chicken == null || chicken.isDead() || player == null) return;

        // Pick a random cursed effect
        CursedEffect[] effects = CursedEffect.values();
        CursedEffect chosen = effects[random.nextInt(effects.length)];

        player.addPotionEffect(new PotionEffect(
                chosen.effectType,
                chosen.duration,
                0,  // amplifier level 0
                false,
                true,
                true
        ));
    }

    @Override
    public double getProximityRange() {
        return PROXIMITY_RANGE;
    }
}
