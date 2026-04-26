package com.chaoschickens.forge.trait;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

/**
 * Golden chicken trait. Drops random ores instead of eggs and glows.
 */
public class GoldenTrait extends ForgeTrait {

    public GoldenTrait() {
        super(TraitType.GOLDEN, "Drops precious ores instead of eggs!", 0.5,
                false, false, 20);
    }

    @Override
    public boolean modifiesDrops() {
        return true;
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Component.literal("Golden Chicken").withStyle(ChatFormatting.GOLD));
        chicken.setCustomNameVisible(true);
        chicken.setGlowing(true);
    }

    @Override
    public void onDeath(Chicken chicken, DamageSource source) {
        if (chicken == null) return;
        dropOreItems(chicken);
    }

    @Override
    public void onModifyDrops(Chicken chicken, LivingDropsEvent event) {
        if (chicken == null) return;
        // Clear default drops and add ore drops
        event.getDrops().clear();
        dropOreItems(chicken);
    }

    private void dropOreItems(Chicken chicken) {
        var random = chicken.getRandom();
        int oreCount = 1 + random.nextInt(3);
        for (int i = 0; i < oreCount; i++) {
            double roll = random.nextDouble();
            ItemStack oreDrop;
            if (roll < 0.40) {
                oreDrop = new ItemStack(Items.GOLD_NUGGET, 2 + random.nextInt(4));
            } else if (roll < 0.65) {
                oreDrop = new ItemStack(Items.GOLD_INGOT, 1);
            } else if (roll < 0.85) {
                oreDrop = new ItemStack(Items.IRON_INGOT, 1);
            } else if (roll < 0.95) {
                oreDrop = new ItemStack(Items.EMERALD, 1);
            } else {
                oreDrop = new ItemStack(Items.DIAMOND, 1);
            }
            chicken.spawnAtLocation(oreDrop);
        }
    }
}
