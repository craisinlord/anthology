package com.craisinlord.anthology.mixins.portal.client;

import com.craisinlord.anthology.portal.client.PortalViewRenderer;
import com.craisinlord.anthology.portal.client.PortalStencilTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererPortalLifecycleMixin {
    @Shadow
    private PostChain transparencyChain;

    @Shadow
    protected abstract void initTransparency();

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void anthology$preparePortalStencil(CallbackInfo ci) {
        if (PortalViewRenderer.isEnabled()) {
            RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
            if (PortalStencilTarget.prepareMainTarget(mainTarget) && this.transparencyChain != null) {
                this.initTransparency();
                mainTarget.bindWrite(true);
            }
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
