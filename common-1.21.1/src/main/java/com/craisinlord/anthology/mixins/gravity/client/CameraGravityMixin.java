package com.craisinlord.anthology.mixins.gravity.client;

import com.craisinlord.anthology.gravity.GravityHooks;
import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityRotationUtil;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraGravityMixin {
    @Shadow
    private Entity entity;

    @Shadow
    private Quaternionf rotation;

    @Shadow
    private float eyeHeight;

    @Shadow
    private float eyeHeightOld;

    private float anthology$storedTickDelta;

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setup", at = @At("HEAD"))
    private void anthology$storeTickDelta(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
        this.anthology$storedTickDelta = partialTick;
    }

    @Redirect(
            method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V")
    )
    private void anthology$moveGravityCameraAnchor(
            Camera camera,
            double x,
            double y,
            double z,
            BlockGetter level,
            Entity entity,
            boolean detached,
            boolean mirrored,
            float partialTick
    ) {
        if (entity == null) {
            this.setPosition(x, y, z);
            return;
        }

        float flipProgress = GravityApi.getGravityFlipProgress(entity, partialTick);
        if (!GravityApi.isGravityInverted(entity) && flipProgress <= 0.0F) {
            this.setPosition(x, y, z);
            return;
        }

        Vec3 interpolatedPos = entity.getPosition(partialTick);
        float normalEyeHeight = this.eyeHeightOld + (this.eyeHeight - this.eyeHeightOld) * partialTick;
        Vector3f eyeOffset = new Vector3f(0.0F, normalEyeHeight, 0.0F);
        eyeOffset.rotate(GravityRotationUtil.getWorldRotationQuaternion(entity, partialTick));
        this.setPosition(
                interpolatedPos.x + eyeOffset.x(),
                interpolatedPos.y + eyeOffset.y(),
                interpolatedPos.z + eyeOffset.z()
        );
    }

    @Inject(
            method = {"setRotation(FF)V", "setRotation(FFF)V"},
            at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;", shift = At.Shift.AFTER)
    )
    private void anthology$applyGravityRotation(CallbackInfo ci) {
        if (this.entity == null) {
            return;
        }
        if (GravityApi.isGravityInverted(this.entity) || GravityApi.getGravityFlipProgress(this.entity, this.anthology$storedTickDelta) > 0.0F) {
            this.rotation.premul(GravityRotationUtil.getCameraRotationQuaternion(this.entity, this.anthology$storedTickDelta));
        }
        this.rotation.premul(new Quaternionf().fromAxisAngleDeg(0.0F, 0.0F, -1.0F, GravityHooks.cameraRollDegrees(this.anthology$storedTickDelta)));
    }
}
