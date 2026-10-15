package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ThrowableProjectile.class)
public abstract class ThrowableProjectileGravityMixin {
    @Shadow
    protected abstract float getGravity();

    @WrapOperation(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/ThrowableProjectile;setDeltaMovement(DDD)V"))
    private void anthology$applyThrownProjectileGravityUpward(ThrowableProjectile self, double x, double y, double z, Operation<Void> original) {
        if (GravityHooks.shouldInvertProjectileGravity(self)) {
            y += 2.0D * this.getGravity();
        }
        original.call(self, x, y, z);
    }
}
