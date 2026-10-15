package com.craisinlord.anthology.mixins.portal.client;

import com.mojang.renderpearl.api.textures.GpuSampler;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererPortalViewAreaAccessor {
    @Accessor("chunkLayerSampler")
    GpuSampler anthology$getChunkLayerSampler();
}
