package com.chaoschickens.fabric.mixin;

import com.chaoschickens.common.trait.TraitType;
import com.chaoschickens.fabric.ChaosChickensFabric;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into LivingEntity to handle drop modifications for chaos chickens.
 * Specifically handles the Golden trait's ore drops.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * Inject after dropLoot to modify drops for golden chickens.
     * Replaces standard egg/feather drops with random ores.
     */
    @Inject(method = "dropLoot", at = @At("TAIL"))
    private void chaoschickens$modifyDrops(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Only process for chickens with the GOLDEN trait
        if (!(self instanceof ChickenEntity chicken)) return;

        ChaosChickensFabric.getActiveTrait(chicken).ifPresent(type -> {
            if (type != TraitType.GOLDEN) return;

            Random random = chicken.getWorld().getRandom();
            // Drop weighted random ores
            int oreCount = 1 + random.nextInt(3); // 1-3 ore items
            for (int i = 0; i < oreCount; i++) {
                double roll = random.nextDouble();
                ItemStack oreDrop;
                if (roll < 0.40) {
                    oreDrop = new ItemStack(Items.GOLD_NUGGET, 2 + random.nextInt(4)); // 2-5 nuggets
                } else if (roll < 0.65) {
                    oreDrop = new ItemStack(Items.GOLD_INGOT, 1); // 1 gold ingot
                } else if (roll < 0.85) {
                    oreDrop = new ItemStack(Items.IRON_INGOT, 1); // 1 iron ingot
                } else if (roll < 0.95) {
                    oreDrop = new ItemStack(Items.EMERALD, 1); // 1 emerald
                } else {
                    oreDrop = new ItemStack(Items.DIAMOND, 1); // 1 diamond
                }
                chicken.dropStack(oreDrop);
            }
        });
    }
}
