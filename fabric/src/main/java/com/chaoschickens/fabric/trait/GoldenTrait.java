package com.chaoschickens.fabric.trait;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Golden chicken trait.
 * Drops random ores instead of eggs and glows with a golden aura.
 */
public class GoldenTrait extends FabricTrait {

    public GoldenTrait() {
        super(TraitType.GOLDEN, "Drops precious ores instead of eggs!", 0.5,
                false, false, 20);
    }

    @Override
    public boolean modifiesDrops() {
        return true;
    }

    @Override
    public void onApply(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Text.literal("Golden Chicken").formatted(Formatting.GOLD));
        chicken.setCustomNameVisible(true);
        chicken.setGlowing(true);
    }

    @Override
    public void onDeath(ChickenEntity chicken, DamageSource source) {
        if (chicken == null) return;

        var random = chicken.getWorld().getRandom();
        int oreCount = 1 + random.nextInt(3); // 1-3 ore drops

        for (int i = 0; i < oreCount; i++) {
            double roll = random.nextDouble();
            ItemStack oreDrop;
            if (roll < 0.40) {
                oreDrop = new ItemStack(Items.GOLD_NUGGET, 2 + random.nextInt(4)); // 2-5 nuggets
            } else if (roll < 0.65) {
                oreDrop = new ItemStack(Items.GOLD_INGOT, 1);
            } else if (roll < 0.85) {
                oreDrop = new ItemStack(Items.IRON_INGOT, 1);
            } else if (roll < 0.95) {
                oreDrop = new ItemStack(Items.EMERALD, 1);
            } else {
                oreDrop = new ItemStack(Items.DIAMOND, 1);
            }
            chicken.dropStack(oreDrop);
        }
    }
}
