package com.craisinlord.anthology.mixins.portal.client;

import com.craisinlord.anthology.portal.client.PortalSectionViews;
import net.minecraft.client.multiplayer.ClientChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientChunkCache.class)
public abstract class PortalViewChunkCacheMixin {
    @Inject(method = "drop(II)V", at = @At("TAIL"))
    private void anthology$invalidatePortalChunk(int chunkX, int chunkZ, CallbackInfo ci) {
        PortalSectionViews.onChunkDropped(chunkX, chunkZ);
    }
}
