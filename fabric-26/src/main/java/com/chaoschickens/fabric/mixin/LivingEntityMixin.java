package com.chaoschickens.fabric.mixin;

import com.chaoschickens.common.trait.TraitType;
import com.chaoschickens.fabric.ChaosChickensFabric;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void chaoschickens$modifyDrops(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Chicken chicken)) return;
        TraitType trait = ChaosChickensFabric.getActiveTrait(chicken).orElse(TraitType.EMPTY);
        if (trait == TraitType.GOLDEN) {
            RandomSource random = chicken.level().getRandom();
            int oreCount = 1 + random.nextInt(3);
            for (int i = 0; i < oreCount; i++) {
                double roll = random.nextDouble();
                ItemStack oreDrop;
                if (roll < 0.40) { oreDrop = new ItemStack(Items.GOLD_NUGGET, 2 + random.nextInt(4)); }
                else if (roll < 0.65) { oreDrop = new ItemStack(Items.GOLD_INGOT, 1); }
                else if (roll < 0.85) { oreDrop = new ItemStack(Items.IRON_INGOT, 1); }
                else if (roll < 0.95) { oreDrop = new ItemStack(Items.EMERALD, 1); }
                else { oreDrop = new ItemStack(Items.DIAMOND, 1); }
                chicken.spawnAtLocation((ServerLevel) chicken.level(), oreDrop);
            }
            ci.cancel();
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void chaoschickens$onDeath(DamageSource source, CallbackInfo ci) {
        try {
            LivingEntity self = (LivingEntity) (Object) this;
            if (self instanceof Chicken chicken) {
                java.util.Optional<TraitType> traitType = ChaosChickensFabric.getActiveTrait(chicken);
                traitType.ifPresent(type -> {
                    com.chaoschickens.fabric.trait.FabricTrait trait = ChaosChickensFabric.getTraitInstance(type);
                    if (trait != null) { trait.onDeath(chicken, source); }
                });
            }
        } catch (Exception e) { /* No-op */ }
    }
}
