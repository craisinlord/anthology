package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityApi;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityFluidInteraction.class)
public abstract class EntityFluidInteractionGravityMixin {
    @ModifyExpressionValue(method = {"update(Lnet/minecraft/world/entity/Entity;Z)Z", "update(Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;)Z"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getEyeY()D"))
    private double anthology$fixFluidEyeY(double eyeY, @Local(argsOnly = true) Entity entity) {
        return GravityApi.isGravityInverted(entity) ? entity.getEyePosition().y() : eyeY;
    }

    @ModifyExpressionValue(method = {"update(Lnet/minecraft/world/entity/Entity;Z)Z", "update(Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;)Z"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getBlockX()I"))
    private int anthology$fixFluidEyeBlockX(int blockX, @Local(argsOnly = true) Entity entity) {
        return GravityApi.isGravityInverted(entity) ? Mth.floor(entity.getEyePosition().x()) : blockX;
    }

    @ModifyExpressionValue(method = {"update(Lnet/minecraft/world/entity/Entity;Z)Z", "update(Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;)Z"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getBlockZ()I"))
    private int anthology$fixFluidEyeBlockZ(int blockZ, @Local(argsOnly = true) Entity entity) {
        return GravityApi.isGravityInverted(entity) ? Mth.floor(entity.getEyePosition().z()) : blockZ;
    }
}
