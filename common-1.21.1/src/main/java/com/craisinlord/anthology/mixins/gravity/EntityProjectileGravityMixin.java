package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityHooks;
import com.craisinlord.anthology.gravity.GravityApi;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityProjectileGravityMixin {
    @Inject(method = "applyGravity", at = @At("HEAD"), cancellable = true)
    private void anthology$applyProjectileGravityUpward(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Projectile projectile)) {
            return;
        }
        if (projectile.isNoGravity()) {
            return;
        }

        boolean inverted = GravityApi.isGravityInverted(projectile);
        if (!inverted && projectile.getOwner() != null) {
            inverted = GravityApi.isGravityInverted(projectile.getOwner());
        }
        if (!inverted && !GravityHooks.isInInvertedRegion(projectile)) {
            return;
        }

        projectile.setDeltaMovement(
                projectile.getDeltaMovement().add(0.0D, projectile.getGravity(), 0.0D)
        );
        ci.cancel();
    }
}
