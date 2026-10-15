package com.craisinlord.anthology.mixins.portal.client;

import com.craisinlord.anthology.portal.client.PortalSectionViews;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionOcclusionGraph.class)
public abstract class SectionOcclusionGraphPortalMixin {
    @Inject(method = "schedulePropagationFrom", at = @At("TAIL"))
    private void anthology$notifyPortalGraphs(SectionRenderDispatcher.RenderSection section, CallbackInfo ci) {
        PortalSectionViews.onPropagationScheduled((SectionOcclusionGraph) (Object) this, section);
    }
}
