package com.chaoschickens.forge.trait;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Magnet chicken trait.
 * Pulls nearby item entities toward the chicken and has a chance to steal them.
 */
public class MagnetTrait extends ForgeTrait {

    private static final double ATTRACTION_RANGE = 6.0;
    private static final double STEAL_CHANCE = 0.30;
    private static final double PULL_STRENGTH = 0.15;

    public MagnetTrait() {
        super(TraitType.MAGNET, "Steals your items! Keep your loot away!", 0.8,
                false, true, 10);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Component.literal("Magnet Chicken").withStyle(ChatFormatting.DARK_AQUA));
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        if (!(chicken.level() instanceof ServerLevel serverLevel)) return;

        // Find all item entities within range
        List<ItemEntity> nearbyItems = serverLevel.getEntitiesOfClass(
                ItemEntity.class,
                chicken.getBoundingBox().inflate(ATTRACTION_RANGE),
                item -> item.isAlive()
        );

        Vec3 chickenPos = chicken.position();

        for (ItemEntity item : nearbyItems) {
            Vec3 itemPos = item.position();
            Vec3 direction = chickenPos.subtract(itemPos).normalize().scale(PULL_STRENGTH);
            item.setDeltaMovement(item.getDeltaMovement().add(direction));
            item.hurtMarked = true;

            // Chance to steal the item
            if (chicken.getRandom().nextDouble() < STEAL_CHANCE) {
                item.discard();
            }
        }
    }
}
