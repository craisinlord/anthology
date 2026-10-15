package com.craisinlord.anthology.mixins.portal.client;

import com.craisinlord.anthology.portal.client.PortalViewRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererPortalLifecycleMixin {
    @Inject(method = "resetLevelRenderData", at = @At("HEAD"))
    private void anthology$clearPortalViewsOnLevelChange(CallbackInfo ci) {
        PortalViewRenderer.releaseTargets();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void anthology$clearPortalViewsOnClose(CallbackInfo ci) {
        PortalViewRenderer.releaseTargets();
        PortalViewRenderer.close();
    }

    @Inject(method = "invalidateCompiledGeometry", at = @At("TAIL"))
    private void anthology$clearPortalViewsOnRendererRebuild(CallbackInfo ci) {
        PortalViewRenderer.releaseTargets();
    }
}
