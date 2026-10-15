package com.craisinlord.anthology.mixins.gravity.client;

import com.craisinlord.anthology.gravity.GravityRotationUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererGravityViewMixin {
    @Shadow
    @Final
    private Camera mainCamera;

    @Inject(
            method = "renderLevel",
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getYRot()F")),
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V", ordinal = 0, shift = At.Shift.AFTER)
    )
    private void anthology$applyGravityViewRotation(float partialTick, long finishTimeNano, PoseStack poseStack, CallbackInfo ci) {
        Entity entity = this.mainCamera.getEntity();
        if (entity == null) {
            return;
        }
        Quaternionf rotation = GravityRotationUtil.getCameraViewRotation(entity, partialTick);
        if (rotation.equals(new Quaternionf())) {
            return;
        }
        poseStack.mulPose(rotation.conjugate());
    }
}
