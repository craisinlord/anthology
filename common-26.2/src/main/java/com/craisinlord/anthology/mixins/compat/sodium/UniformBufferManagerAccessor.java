package com.craisinlord.anthology.mixins.compat.sodium;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = UniformBufferManager.class, remap = false)
public interface UniformBufferManagerAccessor {
    @Accessor(value = "hasUpdatedThisFrame", remap = false)
    boolean anthology$getHasUpdatedThisFrame();

    @Accessor(value = "hasUpdatedThisFrame", remap = false)
    void anthology$setHasUpdatedThisFrame(boolean hasUpdatedThisFrame);

    @Accessor(value = "uniformData", remap = false)
    GpuBufferSlice anthology$getUniformData();

    @Accessor(value = "uniformData", remap = false)
    void anthology$setUniformData(GpuBufferSlice uniformData);
}
