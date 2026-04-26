package com.chaoschickens.forge.trait;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;

/**
 * Cursed chicken trait. Applies random negative status effects to nearby players.
 */
public class CursedTrait extends ForgeTrait {

    private static final double PROXIMITY_RANGE = 6.0;
    private static final int LONG_DURATION = 200;  // 10 seconds
    private static final int SHORT_DURATION = 100;  // 5 seconds

    public CursedTrait() {
        super(TraitType.CURSED, "Curses nearby players with random bad effects!", 0.7,
                true, true, 40);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Component.literal("Cursed Chicken").withStyle(ChatFormatting.DARK_PURPLE));
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.level() instanceof ServerLevel serverLevel)) return;

        // Curse particles
        if (chicken.tickCount % 20 == 0) {
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    chicken.getX(), chicken.getY() + 0.5, chicken.getZ(),
                    4, 0.3, 0.3, 0.3, 0.02);
        }
    }

    @Override
    public void onPlayerNear(Chicken chicken, Player player) {
        if (player == null || player.isDeadOrDying()) return;

        var random = chicken.getRandom();
        int effectIndex = random.nextInt(5);
        MobEffectInstance effect = switch (effectIndex) {
            case 0 -> new MobEffectInstance(MobEffects.POISON, LONG_DURATION, 0);
            case 1 -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, LONG_DURATION, 0);
            case 2 -> new MobEffectInstance(MobEffects.WEAKNESS, LONG_DURATION, 0);
            case 3 -> new MobEffectInstance(MobEffects.BLINDNESS, SHORT_DURATION, 0);
            case 4 -> new MobEffectInstance(MobEffects.WITHER, SHORT_DURATION, 0);
            default -> new MobEffectInstance(MobEffects.POISON, LONG_DURATION, 0);
        };

        player.addEffect(effect);
    }

    @Override
    public double getProximityRange() {
        return PROXIMITY_RANGE;
    }
}
