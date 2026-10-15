package com.craisinlord.anthology.mixins.portal.client;

import com.craisinlord.anthology.portal.client.PortalSectionViews;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererPortalViewAreaMixin {
    @Inject(method = "addRecentlyCompiledSection", at = @At("TAIL"))
    private void anthology$notifyPortalGraphs(SectionRenderDispatcher.RenderSection section, CallbackInfo ci) {
        PortalSectionViews.onSectionCompiled(section);
    }

    @Inject(method = "onChunkReadyToRender", at = @At("TAIL"))
    private void anthology$updatePortalChunkGraph(ChunkPos pos, CallbackInfo ci) {
        PortalSectionViews.onChunkLoaded(pos);
    }
}
