package com.craisinlord.anthology.mixins.gravity.client;

import com.craisinlord.anthology.Anthology;
import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityRotationUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherGravityMixin {
    @org.spongepowered.asm.mixin.Unique
    private static boolean anthology$loggedGravityRenderFailure;

    @Inject(
            method = "render",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 0, shift = At.Shift.AFTER)
    )
    private <E extends Entity> void anthology$rotateEntityRenderPose(
            E entity,
            double x,
            double y,
            double z,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo ci
    ) {
        try {
            float flipProgress = GravityApi.getGravityFlipProgress(entity, partialTick);
            if (!GravityApi.isGravityInverted(entity) && flipProgress <= 0.0F) {
                return;
            }

            poseStack.mulPose(GravityRotationUtil.getCameraRotationQuaternion(entity, partialTick));
        } catch (Throwable throwable) {
            anthology$logGravityRenderFailure(entity, throwable);
        }
    }

    @ModifyVariable(
            method = "renderHitbox",
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/phys/AABB;move(DDD)Lnet/minecraft/world/phys/AABB;", ordinal = 0),
            ordinal = 0
    )
    private static AABB anthology$rotateRenderedHitbox(
            AABB box,
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            Entity entity,
            float red,
            float green,
            float blue,
            float partialTick
    ) {
        try {
            if (!GravityApi.isGravityInverted(entity)) {
                return box;
            }

            return GravityRotationUtil.boxWorldToPlayer(
                    box,
                    GravityApi.getGravityDirection(entity)
            );
        } catch (Throwable throwable) {
            anthology$logGravityRenderFailure(entity, throwable);
            return box;
        }
    }

    @WrapOperation(
            method = "renderHitbox",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 0)
    )
    private static void anthology$rotateRenderedPartOffset(
            PoseStack poseStack,
            double x,
            double y,
            double z,
            Operation<Void> original,
            @Local(argsOnly = true) Entity entity
    ) {
        if (GravityApi.isGravityInverted(entity)) {
            Vec3 offset = GravityRotationUtil.vecWorldToPlayer(new Vec3(x, y, z), GravityApi.getGravityDirection(entity));
            original.call(poseStack, offset.x, offset.y, offset.z);
            return;
        }
        original.call(poseStack, x, y, z);
    }

    @WrapOperation(
            method = "renderHitbox",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;move(DDD)Lnet/minecraft/world/phys/AABB;", ordinal = 1)
    )
    private static AABB anthology$rotateRenderedPartHitbox(
            AABB box,
            double x,
            double y,
            double z,
            Operation<AABB> original,
            @Local(argsOnly = true) Entity entity
    ) {
        AABB moved = original.call(box, x, y, z);
        return GravityApi.isGravityInverted(entity)
                ? GravityRotationUtil.boxWorldToPlayer(moved, GravityApi.getGravityDirection(entity))
                : moved;
    }

    @WrapOperation(
            method = "renderHitbox",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewVector(F)Lnet/minecraft/world/phys/Vec3;", ordinal = 0)
    )
    private static Vec3 anthology$rotateRenderedHitboxViewVector(Entity entity, float partialTick, Operation<Vec3> original) {
        Vec3 viewVector = original.call(entity, partialTick);
        try {
            if (!GravityApi.isGravityInverted(entity)) {
                return viewVector;
            }

            return GravityRotationUtil.vecWorldToPlayer(
                    viewVector,
                    GravityApi.getGravityDirection(entity)
            );
        } catch (Throwable throwable) {
            anthology$logGravityRenderFailure(entity, throwable);
            return viewVector;
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private static void anthology$logGravityRenderFailure(Entity entity, Throwable throwable) {
        if (anthology$loggedGravityRenderFailure) {
            return;
        }

        anthology$loggedGravityRenderFailure = true;
        Anthology.LOGGER.error("Disabling Anthology gravity render adjustments after a client render failure on entity {}", entity, throwable);
    }
}
