package com.chaoschickens.fabric.trait;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Cursed chicken trait.
 * Applies random negative status effects to nearby players.
 */
public class CursedTrait extends FabricTrait {

    /** Proximity range for cursing players. */
    private static final double PROXIMITY_RANGE = 6.0;

    /** Effect durations in ticks. */
    private static final int LONG_DURATION = 200;  // 10 seconds
    private static final int SHORT_DURATION = 100;  // 5 seconds

    public CursedTrait() {
        super(TraitType.CURSED, "Curses nearby players with random bad effects!", 0.7,
                true, true, 40);
    }

    @Override
    public void onApply(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Text.literal("Cursed Chicken").formatted(Formatting.DARK_PURPLE));
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.getWorld() instanceof ServerWorld serverWorld)) return;

        // Spawn curse particles around the chicken
        if (chicken.age % 20 == 0) {
            serverWorld.spawnParticles(
                    ParticleTypes.WITCH,
                    chicken.getX(), chicken.getY() + 0.5, chicken.getZ(),
                    4, 0.3, 0.3, 0.3, 0.02
            );
        }
    }

    @Override
    public void onPlayerNear(ChickenEntity chicken, PlayerEntity player) {
        if (player == null || player.isDead()) return;

        // Apply a random negative status effect
        var random = chicken.getRandom();
        int effectIndex = random.nextInt(5);
        StatusEffectInstance effect = switch (effectIndex) {
            case 0 -> new StatusEffectInstance(StatusEffects.POISON, LONG_DURATION, 0);
            case 1 -> new StatusEffectInstance(StatusEffects.SLOWNESS, LONG_DURATION, 0);
            case 2 -> new StatusEffectInstance(StatusEffects.WEAKNESS, LONG_DURATION, 0);
            case 3 -> new StatusEffectInstance(StatusEffects.BLINDNESS, SHORT_DURATION, 0);
            case 4 -> new StatusEffectInstance(StatusEffects.WITHER, SHORT_DURATION, 0);
            default -> new StatusEffectInstance(StatusEffects.POISON, LONG_DURATION, 0);
        };

        player.addStatusEffect(effect);
    }

    @Override
    public double getProximityRange() {
        return PROXIMITY_RANGE;
    }
}
