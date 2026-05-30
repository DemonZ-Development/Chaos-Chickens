package com.chaoschickens.fabric.mixin;

import com.chaoschickens.common.trait.TraitType;
import com.chaoschickens.fabric.ChaosChickensFabric;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin {

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void chaoschickens$finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.EntitySpawnReason spawnType, net.minecraft.world.entity.SpawnGroupData spawnGroupData, CallbackInfoReturnable<net.minecraft.world.entity.SpawnGroupData> cir) {
        Mob self = (Mob) (Object) this;
        net.minecraft.server.level.ServerLevel serverLevel = level.getLevel();
        long time = 0;
        try {
            try {
                time = (long) serverLevel.getClass().getMethod("getDayTime").invoke(serverLevel);
            } catch (NoSuchMethodException e) {
                try {
                    time = (long) serverLevel.getClass().getMethod("dayTime").invoke(serverLevel);
                } catch (NoSuchMethodException e2) {
                    time = serverLevel.getOverworldClockTime();
                }
            }
        } catch (Exception e) {
            time = serverLevel.getOverworldClockTime();
        }
        long timeOfDay = time % 24000;
        boolean isNight = timeOfDay >= 13000 && timeOfDay <= 23000;

        // If it is night and a monster spawns naturally, 10% chance to replace with a zombie chicken
        if (!(self instanceof Chicken) && self instanceof Monster && spawnType == net.minecraft.world.entity.EntitySpawnReason.NATURAL) {
            if (isNight && level.getRandom().nextDouble() < 0.10) {
                Chicken chicken = net.minecraft.world.entity.EntityType.CHICKEN.create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.NATURAL);
                if (chicken != null) {
                    chicken.setPos(self.getX(), self.getY(), self.getZ());
                    chicken.setYRot(self.getYRot());
                    chicken.setXRot(self.getXRot());
                    ChaosChickensFabric.assignTrait(chicken, TraitType.ZOMBIE);
                    level.addFreshEntity(chicken);
                    self.discard();
                    return;
                }
            }
        }

        if (!(self instanceof Chicken chicken)) {
            return;
        }
        
        if (ChaosChickensFabric.isCheckedChicken(chicken.getUUID()) || ChaosChickensFabric.getActiveTrait(chicken).isPresent()) {
            return;
        }

        ChaosChickensFabric.addCheckedChicken(chicken.getUUID());

        if (chicken.isBaby()) {
            ChaosChickensFabric.assignTrait(chicken, null);
            return;
        }

        // Natural spawns at night are always Zombie Chickens
        if (spawnType == net.minecraft.world.entity.EntitySpawnReason.NATURAL && isNight) {
            ChaosChickensFabric.assignTrait(chicken, TraitType.ZOMBIE);
            return;
        }

        if (com.chaoschickens.fabric.util.ConfigLoader.getConfig().isOnlyNaturalSpawns()) {
            if (spawnType != net.minecraft.world.entity.EntitySpawnReason.NATURAL &&
                spawnType != net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE &&
                spawnType != net.minecraft.world.entity.EntitySpawnReason.COMMAND) {
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
