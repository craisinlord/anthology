package com.craisinlord.anthology.fabric.mixin.client;

import com.craisinlord.anthology.shake.client.CameraShake;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
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
    private void anthology$applyCameraShake(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (this.minecraft.level == null || this.minecraft.player == null) {
            CameraShake.clear();
            return;
        }
        float partialTick = this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        CameraShake.Angles angles = CameraShake.angles(cameraState.pos, this.minecraft.player.tickCount + partialTick);
        if (angles != null) {
            poseStack.rotate(Axis.YP.rotationDegrees(angles.yaw()));
            poseStack.rotate(Axis.XP.rotationDegrees(angles.pitch()));
            poseStack.rotate(Axis.ZP.rotationDegrees(angles.roll()));
        }
    }
}
