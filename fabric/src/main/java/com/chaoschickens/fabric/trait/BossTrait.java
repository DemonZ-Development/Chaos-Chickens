package com.chaoschickens.fabric.trait;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class BossTrait extends FabricTrait {

    public BossTrait() {
        super(TraitType.BOSS, "A powerful chicken with multiple chaos traits combined!", 0.02,
                true, true, 100);
    }

    @Override
    public void onApply(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Text.literal("BOSS Chicken").formatted(Formatting.DARK_RED, Formatting.BOLD));
        chicken.setCustomNameVisible(true);
        // Boss chickens have 3x health
        if (chicken.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH) != null) {
            chicken.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(12.0);
            chicken.setHealth(12.0f);
        }
    }

    @Override
    public void onTick(ChickenEntity chicken) {
        if (chicken == null || chicken.isRemoved() || chicken.isDead()) return;
        chicken.getWorld().addParticle(ParticleTypes.DRAGON_BREATH,
            chicken.getX(), chicken.getY() + 1, chicken.getZ(),
            0, 0.05, 0);
    }

    @Override
    public void onDeath(ChickenEntity chicken, net.minecraft.entity.damage.DamageSource source) {
        if (chicken == null) return;
        chicken.getWorld().addParticle(ParticleTypes.EXPLOSION_EMITTER,
            chicken.getX(), chicken.getY(), chicken.getZ(),
            0, 0, 0);
    }

    @Override
    public double getProximityRange() {
        return 16.0;
    }

    @Override
    public void onPlayerNear(ChickenEntity chicken, PlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 0));
    }
}
