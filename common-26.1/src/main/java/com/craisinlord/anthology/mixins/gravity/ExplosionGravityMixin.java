package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityDirection;
import com.craisinlord.anthology.gravity.GravityRotationUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerExplosion.class)
public abstract class ExplosionGravityMixin {
    @WrapOperation(method = {"hurtEntities()V", "hurtEntities(Ljava/util/List;)V"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;push(Lnet/minecraft/world/phys/Vec3;)V"))
    private void anthology$fixExplosionKnockback(Entity entity, Vec3 knockback, Operation<Void> original) {
        GravityDirection direction = GravityApi.getGravityDirection(entity);
        if (!direction.isInverted()) {
            original.call(entity, knockback);
            return;
        }
        original.call(entity, GravityRotationUtil.vecWorldToPlayer(knockback, direction));
    }
}
