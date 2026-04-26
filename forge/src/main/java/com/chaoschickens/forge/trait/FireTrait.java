package com.chaoschickens.forge.trait;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;

/**
 * Fire chicken trait.
 * Sets nearby players on fire and periodically spawns flame particles.
 */
public class FireTrait extends ForgeTrait {

    private static final double PROXIMITY_RANGE = 5.0;
    private static final int FIRE_DURATION = 60; // 3 seconds in ticks

    public FireTrait() {
        super(TraitType.FIRE, "A fiery chicken that sets nearby players ablaze!", 1.0,
                true, true, 40);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Component.literal("Fire Chicken").withStyle(ChatFormatting.GOLD));
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.level() instanceof ServerLevel serverLevel)) return;

        // Spawn flame particles
        if (chicken.tickCount % 10 == 0) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    chicken.getX(), chicken.getY() + 0.5, chicken.getZ(),
                    5, 0.3, 0.3, 0.3, 0.02);
        }
    }

    @Override
    public void onPlayerNear(Chicken chicken, Player player) {
        if (player == null || player.isDeadOrDying()) return;
        player.setSecondsOnFire(3);
    }

    @Override
    public double getProximityRange() {
        return PROXIMITY_RANGE;
    }
}
