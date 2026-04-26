package com.chaoschickens.fabric.trait;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Disco chicken trait.
 * Plays random note block sounds and dyes nearby sheep in random colors.
 */
public class DiscoTrait extends FabricTrait {

    /** Range to find sheep for dyeing. */
    private static final double SHEEP_DYE_RANGE = 6.0;

    public DiscoTrait() {
        super(TraitType.DISCO, "Party time! Plays music and dyes sheep!", 1.0,
                false, true, 15);
    }

    @Override
    public void onApply(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Text.literal("Disco Chicken").formatted(Formatting.LIGHT_PURPLE));
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.getWorld() instanceof ServerWorld serverWorld)) return;

        // Play random note block sound
        float pitch = 0.5f + chicken.getRandom().nextFloat() * 1.5f;
        serverWorld.playSound(
                null,
                chicken.getX(), chicken.getY(), chicken.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_HARP,
                net.minecraft.sound.SoundCategory.NEUTRAL,
                0.5f, pitch
        );

        // Spawn colorful particles
        DyeColor randomColor = DyeColor.values()[chicken.getRandom().nextInt(DyeColor.values().length)];
        double r = (randomColor.getColorComponent()[0]);
        double g = (randomColor.getColorComponent()[1]);
        double b = (randomColor.getColorComponent()[2]);

        serverWorld.spawnParticles(
                ParticleTypes.ENTITY_EFFECT,
                chicken.getX(), chicken.getY() + 0.5, chicken.getZ(),
                8, 0.4, 0.4, 0.4, 1.0
        );

        // Dye nearby sheep every 2 seconds (30 ticks at interval 15 = every 2 ticks, so gate by age)
        if (chicken.age % 30 == 0) {
            List<SheepEntity> nearbySheep = serverWorld.getEntitiesByClass(
                    SheepEntity.class,
                    chicken.getBoundingBox().expand(SHEEP_DYE_RANGE),
                    sheep -> sheep.isAlive()
            );

            for (SheepEntity sheep : nearbySheep) {
                DyeColor newColor = DyeColor.values()[chicken.getRandom().nextInt(DyeColor.values().length)];
                sheep.setColor(newColor);
            }
        }
    }
}
