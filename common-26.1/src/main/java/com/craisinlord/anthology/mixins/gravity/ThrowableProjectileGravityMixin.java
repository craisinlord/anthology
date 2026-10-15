package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ThrowableProjectile.class)
public abstract class ThrowableProjectileGravityMixin {
    @WrapOperation(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/ThrowableProjectile;applyGravity()V"))
    private void anthology$applyThrownProjectileGravityUpward(ThrowableProjectile self, Operation<Void> original) {
        if (!GravityHooks.isInInvertedRegion(self)) {
            original.call(self);
            return;
        }

        double gravity = self.getGravity();
        self.setDeltaMovement(self.getDeltaMovement().add(0.0D, gravity, 0.0D));
    }
}
