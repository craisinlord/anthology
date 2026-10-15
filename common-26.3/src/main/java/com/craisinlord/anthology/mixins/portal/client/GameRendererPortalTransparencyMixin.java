package com.craisinlord.anthology.mixins.portal.client;

import com.craisinlord.anthology.portal.client.PortalSceneRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererPortalTransparencyMixin {
    @Inject(method = "useImprovedTransparency", at = @At("HEAD"), cancellable = true)
    private void anthology$disableImprovedTransparencyInPortalScenes(CallbackInfoReturnable<Boolean> cir) {
        if (PortalSceneRenderer.isRenderingScene()) {
            cir.setReturnValue(false);
        }
    }
}
