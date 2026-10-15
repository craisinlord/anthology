package com.craisinlord.anthology.mixins.gravity.client;

import com.craisinlord.anthology.Anthology;
import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityRotationUtil;
import com.craisinlord.anthology.gravity.client.GravityRenderState;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherGravityMixin {
    @Unique
    private static boolean anthology$loggedGravityRenderFailure;

    @ModifyReturnValue(method = "extractEntity", at = @At("RETURN"))
    private <E extends Entity> EntityRenderState anthology$captureGravityRotation(EntityRenderState state, E entity, float partialTicks) {
        Quaternionf rotation = null;
        try {
            if (GravityApi.isGravityInverted(entity) || GravityApi.getGravityFlipProgress(entity, partialTicks) > 0.0F) {
                rotation = new Quaternionf(GravityRotationUtil.getCameraRotationQuaternion(entity, partialTicks));
            }
        } catch (Throwable throwable) {
            anthology$logGravityRenderFailure(entity, throwable);
        }
        ((GravityRenderState) state).anthology$setGravityRotation(rotation);
        return state;
    }

    @Inject(
            method = "submit",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 0, shift = At.Shift.AFTER)
    )
    private <S extends EntityRenderState> void anthology$rotateEntityRenderPose(
            S renderState,
            CameraRenderState camera,
            double x,
            double y,
            double z,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CallbackInfo ci
    ) {
        Quaternionf rotation = ((GravityRenderState) renderState).anthology$getGravityRotation();
        if (rotation != null) {
            poseStack.rotate(rotation);
        }
    }

    @Unique
    private static void anthology$logGravityRenderFailure(Entity entity, Throwable throwable) {
        if (anthology$loggedGravityRenderFailure) {
            return;
        }

        anthology$loggedGravityRenderFailure = true;
        Anthology.LOGGER.error("Disabling Anthology gravity render adjustments after a client render failure on entity {}", entity, throwable);
    }
}
