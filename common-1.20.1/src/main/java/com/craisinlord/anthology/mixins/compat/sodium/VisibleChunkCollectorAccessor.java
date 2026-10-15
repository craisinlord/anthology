package com.craisinlord.anthology.mixins.compat.sodium;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.VisibleChunkCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = VisibleChunkCollector.class, remap = false)
public interface VisibleChunkCollectorAccessor {
    @Accessor(value = "sortedRenderLists", remap = false)
    ObjectArrayList<ChunkRenderList> anthology$getSortedRenderLists();
}
