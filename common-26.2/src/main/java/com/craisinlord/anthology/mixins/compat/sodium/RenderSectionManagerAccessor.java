package com.craisinlord.anthology.mixins.compat.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation.UploadResourceBudget;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.executor.ChunkJobCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import net.caffeinemc.mods.sodium.client.render.chunk.storage.SectionStorage;
import net.caffeinemc.mods.sodium.client.render.chunk.tree.RemovableMultiForest;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
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

    @Accessor(value = "renderSections", remap = false)
    SectionStorage anthology$getRenderSections();

    @Invoker(value = "getSearchDistance", remap = false)
    float anthology$getSearchDistance(FogParameters fogParameters);

    @Invoker(value = "invalidateRenderLists", remap = false)
    void anthology$invalidateRenderLists();

    @Invoker(value = "submitSectionTask", remap = false)
    void anthology$submitSectionTask(ChunkJobCollector collector, RenderSection section, UploadResourceBudget budget);
}
