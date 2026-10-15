package com.craisinlord.anthology.mixins.portal.client;

import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.ViewArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererPortalViewAreaAccessor {
    @Accessor("viewArea")
    ViewArea anthology$getViewArea();

    @Accessor("skyRenderer")
    SkyRenderer anthology$getSkyRenderer();

    @Accessor("chunkLayerSampler")
    GpuSampler anthology$getChunkLayerSampler();

    @Accessor("ticks")
    int anthology$getTicks();

    @Accessor("lastCameraSectionX")
    int anthology$getLastCameraSectionX();

    @Accessor("lastCameraSectionY")
    int anthology$getLastCameraSectionY();

    @Accessor("lastCameraSectionZ")
    int anthology$getLastCameraSectionZ();
}
