package com.craisinlord.anthology.mixins.portal.client;

import com.craisinlord.anthology.portal.client.PortalSectionViews;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
public abstract class FrustumPortalOffsetMixin {
    @Inject(method = "offsetToFullyIncludeCameraCube(I)Lnet/minecraft/client/renderer/culling/Frustum;", at = @At("HEAD"), cancellable = true)
    private void anthology$skipPortalViewOffset(int cubeSize, CallbackInfoReturnable<Frustum> cir) {
        if (PortalSectionViews.isCollectingSections()) {
            cir.setReturnValue((Frustum) (Object) this);
        }
    }
}
