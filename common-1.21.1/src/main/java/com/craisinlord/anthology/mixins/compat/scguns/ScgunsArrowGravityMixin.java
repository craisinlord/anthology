package com.craisinlord.anthology.mixins.compat.scguns;

import com.craisinlord.anthology.gravity.GravityHooks;
import com.craisinlord.anthology.gravity.GravityApi;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "top.ribs.scguns.entity.projectile.BrassBoltEntity")
@Pseudo
public abstract class ScgunsArrowGravityMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void anthology$invertScgunsArrowGravity(CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        if (self.isNoGravity() || (!GravityApi.isGravityInverted(self)
                && !GravityHooks.isInInvertedRegion(self))) {
            return;
        }

        self.setDeltaMovement(self.getDeltaMovement().add(0.0D, -0.02D, 0.0D));
    }
}
