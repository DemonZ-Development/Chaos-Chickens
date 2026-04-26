package com.chaoschickens.forge.trait;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Speed chicken trait.
 * Applies a movement speed boost and gives the chicken a yellow name.
 */
public class SpeedTrait extends ForgeTrait {

    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("d3b39e8c-7c2a-4e1f-b8d5-9a6c3e7f1a02");
    private static final String SPEED_MODIFIER_NAME = "ChaosChickensSpeedBoost";

    public SpeedTrait() {
        super(TraitType.SPEED, "A very fast chicken! Zoom zoom!", 1.2,
                false, false, 20);
    }

    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;

        chicken.setCustomName(Component.literal("Speed Chicken").withStyle(ChatFormatting.YELLOW));
        chicken.setCustomNameVisible(true);

        // Apply speed attribute modifier
        var speedAttribute = chicken.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.removeModifier(SPEED_MODIFIER_UUID);
            AttributeModifier speedModifier = new AttributeModifier(
                    SPEED_MODIFIER_UUID,
                    SPEED_MODIFIER_NAME,
                    0.35,
                    AttributeModifier.Operation.ADD_VALUE
            );
            speedAttribute.addPermanentModifier(speedModifier);
        }
    }
}
