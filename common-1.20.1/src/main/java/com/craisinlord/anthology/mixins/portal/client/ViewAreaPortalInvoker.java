package com.craisinlord.anthology.mixins.portal.client;

import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ViewArea.class)
public interface ViewAreaPortalInvoker {
    @Invoker("getRenderChunkAt")
    ChunkRenderDispatcher.RenderChunk anthology$getRenderChunkAt(BlockPos pos);
}
