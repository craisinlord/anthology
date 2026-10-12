package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.client.ClientTimeDilationTicker;
import net.minecraft.client.particle.ReversePortalParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReversePortalParticle.class)
public abstract class ReversePortalParticleTimeDilationMixin {
    @Unique
    private double anthology$timeProgress;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void anthology$slowReversePortalParticle(CallbackInfo ci) {
        ParticlePositionAccessor position = (ParticlePositionAccessor) (Object) this;
        double rate = ClientTimeDilationTicker.rateAt(position.anthology$getX(), position.anthology$getY(), position.anthology$getZ());
        if (rate >= 1.0D) {
            return;
        }
        this.anthology$timeProgress += rate;
        if (this.anthology$timeProgress < 1.0D) {
            ci.cancel();
        } else {
            this.anthology$timeProgress -= 1.0D;
        }
    }
}
