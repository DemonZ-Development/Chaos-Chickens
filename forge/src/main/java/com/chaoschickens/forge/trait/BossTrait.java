package com.chaoschickens.forge.trait;

import com.chaoschickens.common.trait.TraitType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

public class BossTrait extends ForgeTrait {
    
    public BossTrait() {
        super(TraitType.BOSS, "A powerful chicken with multiple chaos traits combined!", 0.02,
                true, true, 100);
    }
    
    @Override
    public void onApply(Chicken chicken) {
        if (chicken == null || chicken.isRemoved()) return;
        chicken.setCustomName(Component.literal("BOSS Chicken").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        chicken.setCustomNameVisible(true);
        if (chicken.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) != null) {
            chicken.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(12.0);
            chicken.setHealth(12.0f);
        }
    }
    
    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isRemoved() || chicken.isDeadOrDying()) return;
        chicken.level().addParticle(ParticleTypes.DRAGON_BREATH,
            chicken.getX(), chicken.getY() + 1, chicken.getZ(),
            0, 0.05, 0);
    }
    
    @Override
    public void onDeath(Chicken chicken, net.minecraft.world.damagesource.DamageSource source) {
        if (chicken == null) return;
        chicken.level().addParticle(ParticleTypes.EXPLOSION_EMITTER,
            chicken.getX(), chicken.getY(), chicken.getZ(),
            0, 0, 0);
    }
    
    @Override
    public double getProximityRange() {
        return 16.0;
    }
    
    @Override
    public void onPlayerNear(Chicken chicken, Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
    }
}
