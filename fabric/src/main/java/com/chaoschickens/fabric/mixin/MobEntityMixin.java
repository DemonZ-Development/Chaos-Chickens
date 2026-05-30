package com.chaoschickens.fabric.mixin;

import com.chaoschickens.common.trait.TraitType;
import com.chaoschickens.fabric.ChaosChickensFabric;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.ChickenEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {

    @Inject(method = "initialize", at = @At("TAIL"))
    private void chaoschickens$initialize(net.minecraft.world.ServerWorldAccess world, net.minecraft.world.LocalDifficulty difficulty, net.minecraft.entity.SpawnReason spawnReason, net.minecraft.entity.EntityData entityData, CallbackInfoReturnable<net.minecraft.entity.EntityData> cir) {
        MobEntity self = (MobEntity) (Object) this;

        // If it is night and a monster spawns naturally, 10% chance to replace with a zombie chicken
        if (!(self instanceof ChickenEntity) && self instanceof HostileEntity && spawnReason == net.minecraft.entity.SpawnReason.NATURAL) {
            if (world.toServerWorld().isNight() && world.getRandom().nextDouble() < 0.10) {
                ChickenEntity chicken = net.minecraft.entity.EntityType.CHICKEN.create(world.toServerWorld());
                if (chicken != null) {
                    chicken.refreshPositionAndAngles(self.getX(), self.getY(), self.getZ(), self.getYaw(), self.getPitch());
                    ChaosChickensFabric.assignTrait(chicken, TraitType.ZOMBIE);
                    world.spawnEntity(chicken);
                    self.discard();
                    return;
                }
            }
        }

        if (!(self instanceof ChickenEntity chicken)) {
            return;
        }
        
        if (ChaosChickensFabric.isCheckedChicken(chicken.getUuid()) || ChaosChickensFabric.getActiveTrait(chicken).isPresent()) {
            return;
        }

        ChaosChickensFabric.addCheckedChicken(chicken.getUuid());

        if (chicken.isBaby()) {
            ChaosChickensFabric.assignTrait(chicken, null);
            return;
        }

        // Natural spawns at night are always Zombie Chickens
        if (spawnReason == net.minecraft.entity.SpawnReason.NATURAL && world.toServerWorld().isNight()) {
            ChaosChickensFabric.assignTrait(chicken, TraitType.ZOMBIE);
            return;
        }

        if (com.chaoschickens.fabric.util.ConfigLoader.getConfig().isOnlyNaturalSpawns()) {
            if (spawnReason != net.minecraft.entity.SpawnReason.NATURAL &&
                spawnReason != net.minecraft.entity.SpawnReason.SPAWN_EGG &&
                spawnReason != net.minecraft.entity.SpawnReason.COMMAND) {
                return;
            }
        }

        if (com.chaoschickens.fabric.util.ConfigLoader.getConfig().isBossChickensEnabled()
                && chicken.getRandom().nextDouble() < com.chaoschickens.fabric.util.ConfigLoader.getConfig().getBossChance()) {
            ChaosChickensFabric.assignTrait(chicken, TraitType.BOSS);
            return;
        }

        double chaosChance = com.chaoschickens.fabric.util.ConfigLoader.getConfig().getChaosChance();
        if (com.chaoschickens.fabric.util.ConfigLoader.getConfig().isForceAllChickensToHaveTraits()
                || chicken.getRandom().nextDouble() < chaosChance) {
            ChaosChickensFabric.assignTrait(chicken, null);
        }
    }
}
