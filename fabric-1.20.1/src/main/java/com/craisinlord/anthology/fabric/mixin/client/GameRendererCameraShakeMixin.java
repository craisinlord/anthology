package com.craisinlord.anthology.fabric.mixin.client;

import com.craisinlord.anthology.shake.client.CameraShake;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererCameraShakeMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "bobView", at = @At("TAIL"))
    private void anthology$applyCameraShake(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (this.minecraft.level == null || this.minecraft.player == null) {
            CameraShake.clear();
            return;
        }
        CameraShake.Angles angles = CameraShake.angles(this.minecraft.gameRenderer.getMainCamera().getPosition(), this.minecraft.player.tickCount + partialTick);
        if (angles != null) {
            poseStack.mulPose(Axis.YP.rotationDegrees(angles.yaw()));
            poseStack.mulPose(Axis.XP.rotationDegrees(angles.pitch()));
            poseStack.mulPose(Axis.ZP.rotationDegrees(angles.roll()));
        }
    }
}
