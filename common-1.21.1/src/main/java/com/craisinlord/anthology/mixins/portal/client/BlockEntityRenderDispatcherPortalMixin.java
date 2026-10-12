package com.craisinlord.anthology.mixins.portal.client;

import com.craisinlord.anthology.portal.client.PortalRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherPortalMixin {
    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity> void anthology$clipPortalViewBlockEntities(E blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        if (PortalRenderState.renderAll() || blockEntity == null || blockEntity.isRemoved()) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        AABB bounds = new AABB(pos);
        if (!PortalRenderState.shouldRenderBounds(bounds)) {
            ci.cancel();
        }
    }
}
