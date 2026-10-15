package com.craisinlord.anthology.mixins.portal.client;

import com.craisinlord.anthology.portal.client.PortalClientTransit;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerPortalTransitMixin {
    @Unique
    private AABB anthology$portalTransitStartBox;
    @Unique
    private Vec3 anthology$portalTransitStartVelocity;

    @Inject(method = "move", at = @At("HEAD"))
    private void anthology$capturePortalTransitStart(MoverType type, Vec3 movement, CallbackInfo ci) {
        this.anthology$portalTransitStartBox = ((LocalPlayer) (Object) this).getBoundingBox();
        this.anthology$portalTransitStartVelocity = ((LocalPlayer) (Object) this).getDeltaMovement();
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void anthology$applyPortalTransit(MoverType type, Vec3 movement, CallbackInfo ci) {
        AABB startBox = this.anthology$portalTransitStartBox;
        Vec3 startVelocity = this.anthology$portalTransitStartVelocity;
        this.anthology$portalTransitStartBox = null;
        this.anthology$portalTransitStartVelocity = null;
        if (startBox != null && startVelocity != null && type == MoverType.SELF) {
            PortalClientTransit.afterLocalMove((LocalPlayer) (Object) this, startBox, startVelocity);
        }
    }
}
