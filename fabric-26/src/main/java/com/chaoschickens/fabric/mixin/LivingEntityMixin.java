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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void chaoschickens$modifyDrops(ServerLevel level, DamageSource source, CallbackInfo ci) {
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
                chicken.spawnAtLocation(level, oreDrop);
            }
            ci.cancel();
        }
    }

    /**
     * Cancel ALL damage for fire chickens from fire/lava/hot floor,
     * and cancel explosion/fire damage for boss chickens.
     * Also triggers onDamage hooks for traits.
     */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void chaoschickens$onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        try {
            LivingEntity self = (LivingEntity) (Object) this;
            if (!(self instanceof Chicken chicken)) return;

            TraitType type = ChaosChickensFabric.getActiveTrait(chicken).orElse(TraitType.EMPTY);
            if (type == TraitType.EMPTY) return;

            // Check if this chicken has fire trait (directly or via boss sub-traits)
            boolean hasFireTrait = (type == TraitType.FIRE);
            boolean isBoss = (type == TraitType.BOSS);

            if (isBoss) {
                java.util.List<TraitType> subTraits = com.chaoschickens.fabric.util.ChickenDataUtil.getBossSubTraits(chicken);
                if (subTraits.contains(TraitType.FIRE)) {
                    hasFireTrait = true;
                }
            }

            // Fire chicken: immune to ALL fire, lava, hot floor, drowning (in lava), and in_fire damage
            if (hasFireTrait) {
                if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                        || chicken.isInLava()
                        || chicken.isOnFire()
                        || source.type().msgId().contains("lava")
                        || source.type().msgId().contains("fire")
                        || source.type().msgId().contains("hotFloor")
                        || source.type().msgId().contains("onFire")
                        || source.type().msgId().contains("inFire")
                        || (source.type().msgId().contains("drown") && chicken.isInLava())) {
                    // Extinguish the chicken too
                    chicken.clearFire();
                    chicken.setRemainingFireTicks(-1);
                    cir.setReturnValue(false);
                    return;
                }
            }

            // Boss chicken: immune to explosions, fire, lava, and drowning in lava
            if (isBoss) {
                if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)
                        || source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                        || chicken.isInLava()
                        || source.type().msgId().contains("lava")
                        || source.type().msgId().contains("fire")) {
                    chicken.clearFire();
                    cir.setReturnValue(false);
                    return;
                }
            }

            // Trigger onDamage hook for the trait
            com.chaoschickens.fabric.trait.FabricTrait trait = ChaosChickensFabric.getTraitInstance(type);
            if (trait != null) {
                trait.onDamage(chicken, source, amount);
            }
        } catch (Exception e) {
            // Silently handle to prevent crashes
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

                    // Grant advancement if killed by player or recently hurt by player
                    net.minecraft.server.level.ServerPlayer player = null;
                    if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        player = sp;
                    } else if (self.getLastHurtByPlayer() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        player = sp;
                    }

                    if (player != null) {
                        net.minecraft.server.MinecraftServer server = player.createCommandSourceStack().getServer();
                        if (server != null) {
                            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                                    "advancement grant " + player.getName().getString() + " only chaoschickens:kill_" + type.getKey());
                        }
                    }
                });
            }
        } catch (Exception e) { /* No-op */ }
    }

    /**
     * Prevent fire chickens from being set on fire at all.
     * This intercepts the base LivingEntity.setRemainingFireTicks to suppress fire for fire chickens.
     */
    @Inject(method = "baseTick", at = @At("TAIL"))
    private void chaoschickens$onBaseTick(CallbackInfo ci) {
        try {
            LivingEntity self = (LivingEntity) (Object) this;
            if (!(self instanceof Chicken chicken)) return;

            TraitType type = ChaosChickensFabric.getActiveTrait(chicken).orElse(TraitType.EMPTY);

            boolean hasFireTrait = (type == TraitType.FIRE);
            if (type == TraitType.BOSS) {
                java.util.List<TraitType> subTraits = com.chaoschickens.fabric.util.ChickenDataUtil.getBossSubTraits(chicken);
                if (subTraits.contains(TraitType.FIRE)) {
                    hasFireTrait = true;
                }
            }

            if (hasFireTrait) {
                // Continuously extinguish fire chicken - this runs EVERY base tick (20x/sec)
                if (chicken.isOnFire() || chicken.getRemainingFireTicks() > 0) {
                    chicken.clearFire();
                    chicken.setRemainingFireTicks(-1);
                }
            }
        } catch (Exception e) { /* No-op */ }
    }
}
