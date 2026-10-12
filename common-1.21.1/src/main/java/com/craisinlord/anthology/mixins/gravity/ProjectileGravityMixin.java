package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.ProjectileLaunchGravity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileGravityMixin {
    @Inject(method = "shootFromRotation", at = @At("HEAD"))
    private void anthology$prepareGravityAwareShot(
            Entity shooter,
            float xRot,
            float yRot,
            float rotationOffset,
            float velocity,
            float inaccuracy,
            CallbackInfo ci
    ) {
        GravityApi.normalizeProjectileFrame((Projectile) (Object) this);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void anthology$keepWorldMovementFrame(CallbackInfo ci) {
        GravityApi.normalizeProjectileFrame((Projectile) (Object) this);
    }

    @WrapOperation(
            method = "shootFromRotation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/Projectile;shoot(DDDFF)V"
            )
    )
    private void anthology$rotateInvertedLaunchDirection(
            Projectile projectile,
            double x,
            double y,
            double z,
            float velocity,
            float inaccuracy,
            Operation<Void> original,
            @Local(argsOnly = true) Entity shooter
    ) {
        Vec3 direction = new Vec3(x, y, z);
        if (GravityApi.isGravityInverted(shooter)) {
            direction = ProjectileLaunchGravity.toWorld(
                    direction,
                    GravityApi.getGravityDirection(shooter)
            );
        }
        original.call(projectile, direction.x, direction.y, direction.z, velocity, inaccuracy);
    }
}
