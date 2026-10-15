package com.craisinlord.anthology.mixins.portal.client;

import java.util.Set;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererPortalViewAreaAccessor {
    @Accessor("viewArea")
    ViewArea anthology$getViewArea();

    @Accessor("globalBlockEntities")
    Set<BlockEntity> anthology$getGlobalBlockEntities();

    @Accessor("lastCameraChunkX")
    int anthology$getLastCameraChunkX();

    @Accessor("lastCameraChunkY")
    int anthology$getLastCameraChunkY();

    @Accessor("lastCameraChunkZ")
    int anthology$getLastCameraChunkZ();
}
