package com.craisinlord.anthology.mixins.compat.sodium;

import com.craisinlord.anthology.portal.client.sodium.SodiumPortalTerrain;
import java.util.ArrayDeque;
import java.util.Map;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkUpdateType;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class RenderSectionManagerPortalTasksMixin {
    @Shadow(remap = false)
    private Map<ChunkUpdateType, ArrayDeque<RenderSection>> rebuildLists;

    @Inject(method = "updateChunks", at = @At("HEAD"), remap = false)
    private void anthology$addPortalViewTasks(boolean updateImmediately, CallbackInfo ci) {
        SodiumPortalTerrain.drainPendingTasks(this.rebuildLists);
    }
}
