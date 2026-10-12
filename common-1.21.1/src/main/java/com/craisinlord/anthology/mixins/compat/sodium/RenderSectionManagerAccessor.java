package com.craisinlord.anthology.mixins.compat.sodium;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortBehavior;
import net.caffeinemc.mods.sodium.client.render.chunk.tree.RemovableMultiForest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = RenderSectionManager.class, remap = false)
public interface RenderSectionManagerAccessor {
    @Accessor(value = "renderLists", remap = false)
    SortedRenderLists anthology$getRenderLists();

    @Accessor(value = "renderLists", remap = false)
    void anthology$setRenderLists(SortedRenderLists renderLists);

    @Accessor(value = "renderableSectionTree", remap = false)
    RemovableMultiForest anthology$getRenderableSectionTree();

    @Accessor(value = "sectionByPosition", remap = false)
    Long2ReferenceMap<RenderSection> anthology$getSectionByPosition();

    @Accessor(value = "sortBehavior", remap = false)
    SortBehavior anthology$getSortBehavior();

    @Invoker(value = "getSearchDistance", remap = false)
    float anthology$getSearchDistance();
}
