package com.craisinlord.anthology.mixins.compat.sodium;

import com.craisinlord.anthology.portal.client.sodium.SodiumPortalTerrain;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation.UploadResourceBudget;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.executor.ChunkJobCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class RenderSectionManagerPortalTasksMixin {
    @Inject(method = "submitDeferredSectionTasks", at = @At("RETURN"), remap = false)
    private void anthology$submitPortalViewTasks(ChunkJobCollector collector, UploadResourceBudget budget, CallbackInfo ci) {
        SodiumPortalTerrain.submitPendingTasks((RenderSectionManagerAccessor) (Object) this, collector, budget);
    }
}
