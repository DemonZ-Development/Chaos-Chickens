package com.chaoschickens.fabric.trait;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/**
 * Explosive chicken trait.
 * Creates a small explosion when the chicken dies, with no block damage.
 */
public class ExplosiveTrait extends FabricTrait {

    /** Explosion power (2.0 = TNT-like, but we disable block damage). */
    private static final float EXPLOSION_POWER = 2.0f;

    public ExplosiveTrait() {
        super(TraitType.EXPLOSIVE, "Explodes when killed! Watch out!", 1.0,
                true, false, 20);
    }

    @Override
    public void onApply(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Text.literal("Explosive Chicken").formatted(Formatting.RED));
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onDeath(ChickenEntity chicken, DamageSource source) {
        if (chicken == null) return;
        World world = chicken.getWorld();
        // Create explosion with no block damage (DestructionType.NONE)
        world.createExplosion(
                chicken,
                chicken.getX(),
                chicken.getY(),
                chicken.getZ(),
                EXPLOSION_POWER,
                World.ExplosionSourceType.MOB
        );
    }
}
