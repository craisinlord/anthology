package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityHooks;
import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityRotationUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BehaviorUtils.class)
public abstract class BehaviorUtilsGravityMixin {
    @WrapOperation(
            method = "throwItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;F)V",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"
            )
    )
    private static ItemEntity anthology$fixThrownItemSpawn(
            Level level,
            double x,
            double y,
            double z,
            ItemStack stack,
            Operation<ItemEntity> original,
            @Local float yOffset,
            @Local LivingEntity entity
    ) {
        if (!anthology$shouldInvertThrownItem(entity)) {
            return original.call(level, x, y, z, stack);
        }

        Vec3 eyeOffset = GravityRotationUtil.getEyeOffset(entity, entity.getEyeHeight());
        Vec3 offset = eyeOffset.normalize().scale(yOffset);
        Vec3 itemPos = entity.position().add(eyeOffset).subtract(offset);
        ItemEntity itemEntity = original.call(level, itemPos.x(), itemPos.y(), itemPos.z(), stack);
        GravityApi.setGravityDirection(itemEntity, GravityApi.getGravityDirection(entity));
        return itemEntity;
    }

    @WrapOperation(
            method = "throwItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/item/ItemEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
            )
    )
    private static void anthology$fixThrownItemVelocity(
            ItemEntity itemEntity,
            Vec3 deltaMovement,
            Operation<Void> original,
            @Local LivingEntity entity
    ) {
        if (!anthology$shouldInvertThrownItem(entity)) {
            original.call(itemEntity, deltaMovement);
            return;
        }

        GravityApi.setWorldVelocity(itemEntity, deltaMovement);
    }

    private static boolean anthology$shouldInvertThrownItem(LivingEntity entity) {
        return GravityHooks.shouldInvertInRegion(entity);
    }
}
