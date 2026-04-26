package com.chaoschickens.fabric.trait;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.block.Blocks;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * Ice chicken trait.
 * Freezes water blocks the chicken walks on, drops ice on death,
 * and spawns snowflake particles periodically.
 */
public class IceTrait extends FabricTrait {

    /** Range to check for water blocks to freeze. */
    private static final int FREEZE_CHECK_RANGE = 1;

    public IceTrait() {
        super(TraitType.ICE, "A freezing chicken that turns water to ice!", 0.8,
                false, true, 20);
    }

    @Override
    public boolean modifiesDrops() {
        return true;
    }

    @Override
    public void onApply(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Text.literal("Ice Chicken").formatted(Formatting.AQUA));
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.getWorld() instanceof ServerWorld serverWorld)) return;

        // Freeze water blocks the chicken walks on
        BlockPos chickenPos = chicken.getBlockPos();
        for (int dx = -FREEZE_CHECK_RANGE; dx <= FREEZE_CHECK_RANGE; dx++) {
            for (int dz = -FREEZE_CHECK_RANGE; dz <= FREEZE_CHECK_RANGE; dz++) {
                BlockPos checkPos = chickenPos.add(dx, -1, dz);
                if (serverWorld.getBlockState(checkPos).isOf(Blocks.WATER)) {
                    serverWorld.setBlockState(checkPos, Blocks.ICE.getDefaultState());
                }
            }
        }

        // Also freeze water at the chicken's feet
        if (serverWorld.getBlockState(chickenPos).isOf(Blocks.WATER)) {
            serverWorld.setBlockState(chickenPos, Blocks.ICE.getDefaultState());
        }

        // Spawn snowflake particles
        if (chicken.age % 15 == 0) {
            serverWorld.spawnParticles(
                    ParticleTypes.SNOWFLAKE,
                    chicken.getX(),
                    chicken.getY() + 0.5,
                    chicken.getZ(),
                    4,
                    0.3, 0.3, 0.3,
                    0.02
            );
        }
    }

    @Override
    public void onDeath(ChickenEntity chicken, DamageSource source) {
        if (chicken == null) return;

        // Drop ice item
        net.minecraft.entity.ItemEntity itemEntity = new net.minecraft.entity.ItemEntity(
                chicken.getWorld(),
                chicken.getX(),
                chicken.getY(),
                chicken.getZ(),
                new ItemStack(Items.ICE, 1 + chicken.getRandom().nextInt(3)) // 1-3 ice
        );
        chicken.getWorld().spawnEntity(itemEntity);
    }
}
