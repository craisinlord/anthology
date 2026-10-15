package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowGravityMixin {
    @Unique
    private static final double ANTHOLOGY$ARROW_GRAVITY = 0.05F;

    @WrapOperation(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;setDeltaMovement(DDD)V"))
    private void anthology$applyArrowGravityUpward(AbstractArrow self, double x, double y, double z, Operation<Void> original) {
        if (GravityHooks.shouldInvertProjectileGravity(self)) {
            y += 2.0D * ANTHOLOGY$ARROW_GRAVITY;
        }
        original.call(self, x, y, z);
    }
}
