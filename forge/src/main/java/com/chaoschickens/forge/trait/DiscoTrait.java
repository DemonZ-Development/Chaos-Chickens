package com.chaoschickens.forge.trait;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;

import java.util.List;

/**
 * Disco chicken trait. Plays note block sounds and dyes nearby sheep.
 */
public class DiscoTrait extends ForgeTrait {

    private static final double SHEEP_DYE_RANGE = 6.0;

    public DiscoTrait() {
        super(TraitType.DISCO, "Party time! Plays music and dyes sheep!", 1.0,
                false, true, 15);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Component.literal("Disco Chicken").withStyle(ChatFormatting.LIGHT_PURPLE));
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.level() instanceof ServerLevel serverLevel)) return;

        // Play random note block sound
        float pitch = 0.5f + chicken.getRandom().nextFloat() * 1.5f;
        serverLevel.playSound(null, chicken.getX(), chicken.getY(), chicken.getZ(),
                SoundEvents.NOTE_BLOCK_HARP.get(), SoundSource.NEUTRAL, 0.5f, pitch);

        // Spawn colorful particles
        serverLevel.sendParticles(ParticleTypes.ENTITY_EFFECT,
                chicken.getX(), chicken.getY() + 0.5, chicken.getZ(),
                8, 0.4, 0.4, 0.4, 1.0);

        // Dye nearby sheep every 2 seconds
        if (chicken.tickCount % 30 == 0) {
            List<Sheep> nearbySheep = serverLevel.getEntitiesOfClass(
                    Sheep.class, chicken.getBoundingBox().inflate(SHEEP_DYE_RANGE), Sheep::isAlive);
            for (Sheep sheep : nearbySheep) {
                DyeColor newColor = DyeColor.values()[chicken.getRandom().nextInt(DyeColor.values().length)];
                sheep.setColor(newColor);
            }
        }
    }
}
