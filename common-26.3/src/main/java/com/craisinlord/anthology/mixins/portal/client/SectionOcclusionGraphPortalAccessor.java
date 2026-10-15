package com.craisinlord.anthology.mixins.portal.client;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SectionOcclusionGraph.class)
public interface SectionOcclusionGraphPortalAccessor {
    @Accessor("emptySections")
    LongOpenHashSet anthology$getEmptySections();

    @Accessor("loadedChunks")
    LongOpenHashSet anthology$getLoadedChunks();
}
