package com.craisinlord.anthology.mixins.portal.client;

import com.craisinlord.anthology.portal.client.PortalSectionViews;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererPortalViewAreaMixin {
    @Inject(method = "addRecentlyCompiledChunk", at = @At("TAIL"))
    private void anthology$notifyPortalGraphs(ChunkRenderDispatcher.RenderChunk section, CallbackInfo ci) {
        PortalSectionViews.onSectionCompiled(section);
    }
}
