package com.craisinlord.anthology.mixins.portal.client;

import com.craisinlord.anthology.portal.client.PortalRenderState;
import com.craisinlord.anthology.portal.client.PortalSectionViews;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererPortalViewAreaMixin {
    @Redirect(method = "renderSnowAndRain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"), require = 0)
    private void anthology$suppressPortalWeatherSound(ClientLevel level, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch, boolean distanceDelay) {
        if (PortalRenderState.getContext() == null) {
            level.playLocalSound(pos, sound, source, volume, pitch, distanceDelay);
        }
    }

    @Inject(method = "addRecentlyCompiledSection", at = @At("TAIL"))
    private void anthology$notifyPortalGraphs(SectionRenderDispatcher.RenderSection section, CallbackInfo ci) {
        PortalSectionViews.onSectionCompiled(section);
    }

    @Inject(method = "onChunkLoaded", at = @At("TAIL"))
    private void anthology$updatePortalChunkGraph(ChunkPos pos, CallbackInfo ci) {
        PortalSectionViews.onChunkLoaded(pos);
    }
}
