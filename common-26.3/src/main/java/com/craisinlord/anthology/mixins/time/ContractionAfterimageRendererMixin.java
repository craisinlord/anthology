package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.client.ContractionAfterimages;
import com.craisinlord.anthology.time.client.TimeDilationRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class ContractionAfterimageRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
    @Unique
    private static final float BASE_ALPHA = 0.42F;
    @Unique
    private static final int SAMPLE_STRIDE = 2;
    @Unique
    private static boolean anthology$capturingAfterimage;

    @Shadow
    protected M model;

    @Shadow
    protected abstract void setupRotations(S state, PoseStack poseStack, float bodyRot, float entityScale);

    @Shadow
    protected abstract void scale(S state, PoseStack poseStack);

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    @SuppressWarnings("unchecked")
    private void anthology$captureAfterimages(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
        TimeDilationRenderState access = (TimeDilationRenderState) state;
        access.anthology$setAfterimages(List.of());
        if (anthology$capturingAfterimage || entity.isSpectator() || !ContractionAfterimages.isActive(entity)) {
            return;
        }
        Deque<ContractionAfterimages.Sample> samples = ContractionAfterimages.samples(entity);
        if (samples == null || samples.isEmpty()) {
            return;
        }
        double curX = Mth.lerp(partialTicks, entity.xo, entity.getX());
        double curY = Mth.lerp(partialTicks, entity.yo, entity.getY());
        double curZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());
        int now = (int) (entity.level().getGameTime() & 0x7fffffffL);
        int rgb = ContractionAfterimages.colorFor(entity);
        LivingEntityRenderer<T, S, M> renderer = (LivingEntityRenderer<T, S, M>) (Object) this;
        List<TimeDilationRenderState.Afterimage> afterimages = new ArrayList<>();
        anthology$capturingAfterimage = true;
        try {
            int index = 0;
            for (ContractionAfterimages.Sample sample : samples) {
                if (index++ % SAMPLE_STRIDE != 0) {
                    continue;
                }
                double dx = sample.x - curX;
                double dy = sample.y - curY;
                double dz = sample.z - curZ;
                if (dx * dx + dy * dy + dz * dz < 0.0025D) {
                    continue;
                }
                float fade = ContractionAfterimages.fade(sample, now);
                if (fade <= 0.02F) {
                    continue;
                }
                S ghost = ((EntityRenderer<T, S>) (Object) this).createRenderState();
                renderer.extractRenderState((T) entity, ghost, partialTicks);
                ghost.bodyRot = sample.bodyYaw;
                ghost.yRot = Mth.wrapDegrees(sample.headYaw - sample.bodyYaw);
                ghost.xRot = sample.xRot;
                ghost.walkAnimationPos = sample.limbPos;
                ghost.walkAnimationSpeed = sample.limbSpeed;
                ghost.ageInTicks = sample.ageInTicks;
                int alpha = (int) (BASE_ALPHA * fade * 255.0F) & 0xFF;
                afterimages.add(new TimeDilationRenderState.Afterimage(ghost, dx, dy, dz, alpha << 24 | rgb));
            }
        } finally {
            anthology$capturingAfterimage = false;
        }
        access.anthology$setAfterimages(afterimages);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"))
    @SuppressWarnings("unchecked")
    private void anthology$submitAfterimages(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                            CameraRenderState camera, CallbackInfo ci) {
        List<TimeDilationRenderState.Afterimage> afterimages = ((TimeDilationRenderState) state).anthology$getAfterimages();
        if (afterimages == null || afterimages.isEmpty()) {
            return;
        }
        LivingEntityRenderer<T, S, M> renderer = (LivingEntityRenderer<T, S, M>) (Object) this;
        Matrix4f inverse = new Matrix4f(poseStack.last().pose()).invert();
        for (TimeDilationRenderState.Afterimage afterimage : afterimages) {
            S ghost = (S) afterimage.state();
            Vector3f local = inverse.transformDirection(new Vector3f((float) afterimage.dx(), (float) afterimage.dy(), (float) afterimage.dz()));
            poseStack.pushPose();
            poseStack.translate(local.x, local.y, local.z);
            float entityScale = ghost.scale;
            poseStack.scale(entityScale, entityScale, entityScale);
            this.setupRotations(ghost, poseStack, ghost.bodyRot, entityScale);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            this.scale(ghost, poseStack);
            poseStack.translate(0.0F, -1.501F, 0.0F);
            Identifier texture = renderer.getTextureLocation(ghost);
            submitNodeCollector.submitModel(this.model, ghost, poseStack, RenderTypes.entityTranslucent(texture), LightCoordsUtil.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, afterimage.color(), null, 0);
            poseStack.popPose();
        }
    }
}
