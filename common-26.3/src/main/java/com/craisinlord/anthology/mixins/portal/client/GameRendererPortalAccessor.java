package com.craisinlord.anthology.mixins.portal.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererPortalAccessor {
    @Accessor("fogRenderer")
    FogRenderer anthology$getFogRenderer();

    @Mutable
    @Accessor("mainRenderTarget")
    void anthology$setMainRenderTarget(RenderTarget renderTarget);
}
