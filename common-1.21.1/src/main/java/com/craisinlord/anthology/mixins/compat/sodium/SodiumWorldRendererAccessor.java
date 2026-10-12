package com.craisinlord.anthology.mixins.compat.sodium;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SodiumWorldRenderer.class, remap = false)
public interface SodiumWorldRendererAccessor {
    @Accessor(value = "renderSectionManager", remap = false)
    RenderSectionManager anthology$getRenderSectionManager();

    @Accessor(value = "useEntityCulling", remap = false)
    boolean anthology$getUseEntityCulling();

    @Accessor(value = "useEntityCulling", remap = false)
    void anthology$setUseEntityCulling(boolean useEntityCulling);
}
