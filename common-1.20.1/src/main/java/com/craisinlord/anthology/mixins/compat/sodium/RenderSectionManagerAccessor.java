package com.craisinlord.anthology.mixins.compat.sodium;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RenderSectionManager.class, remap = false)
public interface RenderSectionManagerAccessor {
    @Accessor(value = "renderLists", remap = false)
    SortedRenderLists anthology$getRenderLists();

    @Accessor(value = "renderLists", remap = false)
    void anthology$setRenderLists(SortedRenderLists renderLists);

    @Accessor(value = "sectionByPosition", remap = false)
    Long2ReferenceMap<RenderSection> anthology$getSectionByPosition();
}
