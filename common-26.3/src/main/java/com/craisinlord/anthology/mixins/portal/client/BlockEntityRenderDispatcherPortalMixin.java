package com.craisinlord.anthology.mixins.portal.client;

import com.craisinlord.anthology.portal.client.PortalRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherPortalMixin {
    @Inject(method = "tryExtractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;FLnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;Z)Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity, S extends BlockEntityRenderState> void anthology$clipPortalViewBlockEntities(E blockEntity, float partialTick, ModelFeatureRenderer.CrumblingOverlay breakProgress, boolean globallyRendered, CallbackInfoReturnable<S> cir) {
        if (anthology$isClipped(blockEntity)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "tryExtractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;FLnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;ZLnet/minecraft/client/renderer/culling/Frustum;)Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;", at = @At("HEAD"), cancellable = true, require = 0)
    private <E extends BlockEntity, S extends BlockEntityRenderState> void anthology$clipPortalViewBlockEntitiesCulled(E blockEntity, float partialTick, ModelFeatureRenderer.CrumblingOverlay breakProgress, boolean globallyRendered, Frustum frustum, CallbackInfoReturnable<S> cir) {
        if (anthology$isClipped(blockEntity)) {
            cir.setReturnValue(null);
        }
    }

    @Unique
    private static boolean anthology$isClipped(BlockEntity blockEntity) {
        if (PortalRenderState.renderAll() || blockEntity == null || blockEntity.isRemoved()) {
            return false;
        }
        BlockPos pos = blockEntity.getBlockPos();
        AABB bounds = new AABB(pos);
        return !PortalRenderState.shouldRenderBounds(bounds);
    }
}
