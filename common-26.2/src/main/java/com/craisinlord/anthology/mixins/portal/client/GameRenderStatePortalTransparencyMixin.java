package com.craisinlord.anthology.mixins.portal.client;

import com.craisinlord.anthology.portal.client.PortalSceneRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderState.class)
public abstract class GameRenderStatePortalTransparencyMixin {
    @Inject(method = "useShaderTransparency", at = @At("HEAD"), cancellable = true)
    private void anthology$disableShaderTransparencyInPortalScenes(CallbackInfoReturnable<Boolean> cir) {
        if (PortalSceneRenderer.isRenderingScene()) {
            cir.setReturnValue(false);
        }
    }
}
