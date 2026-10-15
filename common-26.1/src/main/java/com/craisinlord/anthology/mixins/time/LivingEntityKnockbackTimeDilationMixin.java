package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.TimeDilationEntityAccess;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityKnockbackTimeDilationMixin {
    @Inject(method = "knockback", at = @At("HEAD"))
    private void anthology$beginExternalImpulse(double strength, double x, double z, CallbackInfo ci) {
        if (this instanceof TimeDilationEntityAccess access) {
            access.anthology$setApplyingExternalImpulse(true);
        }
    }

    @Inject(method = "knockback", at = @At("RETURN"))
    private void anthology$endExternalImpulse(double strength, double x, double z, CallbackInfo ci) {
        if (this instanceof TimeDilationEntityAccess access) {
            access.anthology$setApplyingExternalImpulse(false);
        }
    }
}
