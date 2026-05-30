/*
 * Chaos Chickens - Multi-platform Minecraft plugin/mod
 * Copyright (C) 2024-2026 DemonZ Development community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.chaoschickens.fabric.mixin;

import com.chaoschickens.common.trait.TraitType;
import com.chaoschickens.fabric.ChaosChickensFabric;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into LivingEntity to handle drop modifications and death behaviors for chaos chickens.
 * Specifically handles the Golden trait's ore drops and trait death effects.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * Inject at HEAD of dropLoot to modify drops for golden chickens.
     * Cancels default loot drops and replaces them with random ores.
     */
    @Inject(method = "dropLoot", at = @At("HEAD"), cancellable = true)
    private void chaoschickens$modifyDrops(DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Only process for chickens with the GOLDEN trait
        if (!(self instanceof ChickenEntity chicken)) return;

        TraitType trait = ChaosChickensFabric.getActiveTrait(chicken).orElse(TraitType.EMPTY);
        if (trait == TraitType.GOLDEN) {
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
            ci.cancel(); // Cancel standard chicken drops (feathers, raw chicken)!
        }
    }

    /**
     * Inject at HEAD of onDeath to trigger trait-specific death events (e.g., Explosive trait).
     */
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void chaoschickens$onDeath(DamageSource source, CallbackInfo ci) {
        try {
            LivingEntity self = (LivingEntity) (Object) this;
            if (self instanceof ChickenEntity chicken) {
                java.util.Optional<TraitType> traitType = ChaosChickensFabric.getActiveTrait(chicken);
                traitType.ifPresent(type -> {
                    com.chaoschickens.fabric.trait.FabricTrait trait =
                            ChaosChickensFabric.getTraitInstance(type);
                    if (trait != null) {
                        trait.onDeath(chicken, source);
                    }

                    // Grant advancement if killed by player or recently hurt by player
                    net.minecraft.server.network.ServerPlayerEntity player = null;
                    if (source.getAttacker() instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
                        player = sp;
                    } else if (self.getPrimeAdversary() instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
                        player = sp;
                    }

                    if (player != null) {
                        net.minecraft.server.MinecraftServer server = chicken.getServer();
                        if (server != null) {
                            server.getCommandManager().executeWithPrefix(server.getCommandSource(),
                                    "advancement grant " + player.getName().getString() + " only chaoschickens:kill_" + type.getKey());
                        }
                    }
                });
            }
        } catch (Exception e) {
            // No-op, entity cleanup will happen on EntityMixin.remove()
        }
    }

    /**
     * Cancel fire/lava/explosion damage for fire/boss chickens.
     */
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void chaoschickens$onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        try {
            LivingEntity self = (LivingEntity) (Object) this;
            if (!(self instanceof ChickenEntity chicken)) return;

            TraitType type = ChaosChickensFabric.getActiveTrait(chicken).orElse(TraitType.EMPTY);
            if (type == TraitType.EMPTY) return;

            boolean hasFireTrait = (type == TraitType.FIRE);
            boolean isBoss = (type == TraitType.BOSS);

            if (isBoss) {
                java.util.List<TraitType> subTraits = com.chaoschickens.fabric.util.ChickenDataUtil.getBossSubTraits(chicken);
                if (subTraits.contains(TraitType.FIRE)) {
                    hasFireTrait = true;
                }
            }

            if (hasFireTrait) {
                if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_FIRE)
                        || chicken.isInLava()
                        || chicken.isOnFire()
                        || source.isOf(net.minecraft.entity.damage.DamageTypes.IN_FIRE)
                        || source.isOf(net.minecraft.entity.damage.DamageTypes.ON_FIRE)
                        || source.isOf(net.minecraft.entity.damage.DamageTypes.LAVA)
                        || source.isOf(net.minecraft.entity.damage.DamageTypes.HOT_FLOOR)
                        || (source.isOf(net.minecraft.entity.damage.DamageTypes.DROWN) && chicken.isInLava())) {
                    chicken.extinguish();
                    cir.setReturnValue(false);
                    return;
                }
            }

            if (isBoss) {
                if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_EXPLOSION)
                        || source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_FIRE)
                        || chicken.isInLava()
                        || source.isOf(net.minecraft.entity.damage.DamageTypes.LAVA)
                        || source.isOf(net.minecraft.entity.damage.DamageTypes.IN_FIRE)
                        || source.isOf(net.minecraft.entity.damage.DamageTypes.ON_FIRE)) {
                    chicken.extinguish();
                    cir.setReturnValue(false);
                    return;
                }
            }

            com.chaoschickens.fabric.trait.FabricTrait trait = ChaosChickensFabric.getTraitInstance(type);
            if (trait != null) {
                trait.onDamage(chicken, source, amount);
            }
        } catch (Exception e) {
            // Silently handle
        }
    }

    /**
     * Continuously extinguish fire chickens every tick to ensure they do not burn.
     */
    @Inject(method = "baseTick", at = @At("TAIL"))
    private void chaoschickens$onBaseTick(CallbackInfo ci) {
        try {
            LivingEntity self = (LivingEntity) (Object) this;
            if (!(self instanceof ChickenEntity chicken)) return;

            TraitType type = ChaosChickensFabric.getActiveTrait(chicken).orElse(TraitType.EMPTY);
            if (type == TraitType.EMPTY) return;

            boolean hasFireTrait = (type == TraitType.FIRE);
            if (type == TraitType.BOSS) {
                java.util.List<TraitType> subTraits = com.chaoschickens.fabric.util.ChickenDataUtil.getBossSubTraits(chicken);
                if (subTraits.contains(TraitType.FIRE)) {
                    hasFireTrait = true;
                }
            }

            if (hasFireTrait) {
                if (chicken.isOnFire() || chicken.getFireTicks() > 0) {
                    chicken.extinguish();
                }
            }
        } catch (Exception e) {
            // No-op
        }
    }
}
