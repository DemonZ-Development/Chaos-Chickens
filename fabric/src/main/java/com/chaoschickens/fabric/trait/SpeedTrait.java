package com.chaoschickens.fabric.trait;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

/**
 * Speed chicken trait.
 * Applies a movement speed boost to the chicken and gives it a yellow name.
 */
public class SpeedTrait extends FabricTrait {

    /** UUID for the movement speed attribute modifier. */
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("d3b39e8c-7c2a-4e1f-b8d5-9a6c3e7f1a02");

    /** Key for the movement speed attribute modifier. */
    private static final String SPEED_MODIFIER_NAME = "ChaosChickensSpeedBoost";

    public SpeedTrait() {
        super(TraitType.SPEED, "A very fast chicken! Zoom zoom!", 1.2,
                false, false, 20);
    }

    @Override
    public void onApply(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;

        // Set custom name
        chicken.setCustomName(Text.literal("Speed Chicken").formatted(Formatting.YELLOW));
        chicken.setCustomNameVisible(true);

        // Apply speed attribute modifier
        var speedAttribute = chicken.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speedAttribute != null) {
            // Remove existing modifier if present
            EntityAttributeModifier existing = speedAttribute.getModifier(SPEED_MODIFIER_UUID);
            if (existing != null) {
                speedAttribute.removeModifier(existing);
            }

            // Set base movement speed to 0.35 (default is ~0.25 for passive mobs)
            EntityAttributeModifier speedModifier = new EntityAttributeModifier(
                    SPEED_MODIFIER_UUID,
                    SPEED_MODIFIER_NAME,
                    0.35,
                    EntityAttributeModifier.Operation.ADD_VALUE
            );
            speedAttribute.addPersistentModifier(speedModifier);
        }
    }
}
