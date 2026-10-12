package com.craisinlord.anthology.mixins.portal.client;

import com.craisinlord.anthology.portal.client.PortalViewRenderer;
import com.craisinlord.anthology.portal.client.PortalStencilTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererPortalLifecycleMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void anthology$preparePortalStencil(CallbackInfo ci) {
        if (PortalViewRenderer.isEnabled()) {
            PortalStencilTarget.prepareMainTarget(Minecraft.getInstance().getMainRenderTarget());
        }
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void anthology$clearPortalViewsOnLevelChange(ClientLevel level, CallbackInfo ci) {
        PortalViewRenderer.releaseTargets();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void anthology$clearPortalViewsOnClose(CallbackInfo ci) {
        PortalViewRenderer.releaseTargets();
        PortalStencilTarget.clear();
    }

    @Inject(method = "allChanged", at = @At("TAIL"))
    private void anthology$clearPortalViewsOnRendererRebuild(CallbackInfo ci) {
        PortalViewRenderer.releaseTargets();
    }
}
