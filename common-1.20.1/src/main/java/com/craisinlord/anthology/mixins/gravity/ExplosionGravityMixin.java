package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityDirection;
import com.craisinlord.anthology.gravity.GravityRotationUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Explosion.class)
public abstract class ExplosionGravityMixin {
    @WrapOperation(method = "explode",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0))
    private Vec3 anthology$fixExplosionGetVelocity(Entity entity, Operation<Vec3> original) {
        GravityDirection direction = GravityApi.getGravityDirection(entity);
        if (!direction.isInverted()) return original.call(entity);
        return GravityRotationUtil.vecPlayerToWorld(original.call(entity), direction);
    }

    @WrapOperation(method = "explode",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 0))
    private void anthology$fixExplosionSetVelocity(Entity entity, Vec3 velocity, Operation<Void> original) {
        GravityDirection direction = GravityApi.getGravityDirection(entity);
        if (!direction.isInverted()) {
            original.call(entity, velocity);
            return;
        }
        original.call(entity, GravityRotationUtil.vecWorldToPlayer(velocity, direction));
    }
}
