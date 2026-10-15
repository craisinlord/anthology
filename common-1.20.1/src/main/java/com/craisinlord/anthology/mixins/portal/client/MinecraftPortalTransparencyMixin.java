package com.craisinlord.anthology.mixins.portal.client;

import com.craisinlord.anthology.portal.client.PortalSceneRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftPortalTransparencyMixin {
    @Inject(method = "useShaderTransparency", at = @At("HEAD"), cancellable = true)
    private static void anthology$disableShaderTransparencyInPortalScenes(CallbackInfoReturnable<Boolean> cir) {
        if (PortalSceneRenderer.isRenderingScene()) {
            cir.setReturnValue(false);
        }
    }
}
