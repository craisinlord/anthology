package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityHooks;
import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityDirection;
import com.craisinlord.anthology.gravity.GravityRotationUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemGravityMixin {
    private static final double BOLT_OFFSET = 0.15000000596046448D;

    @WrapOperation(method = "shootProjectile",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getX()D", ordinal = 0))
    private static double anthology$fixCrossbowSpawnX(LivingEntity shooter, Operation<Double> original) {
        if (!anthology$shouldFixProjectileSpawn(shooter)) return original.call(shooter);
        GravityDirection direction = GravityApi.getGravityDirection(shooter);
        if (!direction.isInverted()) return original.call(shooter);
        Vec3 offset = GravityRotationUtil.vecPlayerToWorld(0.0D, BOLT_OFFSET, 0.0D, direction);
        return shooter.getEyePosition().subtract(offset).x;
    }

    @WrapOperation(method = "shootProjectile",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getEyeY()D", ordinal = 0))
    private static double anthology$fixCrossbowSpawnY(LivingEntity shooter, Operation<Double> original) {
        if (!anthology$shouldFixProjectileSpawn(shooter)) return original.call(shooter);
        GravityDirection direction = GravityApi.getGravityDirection(shooter);
        if (!direction.isInverted()) return original.call(shooter);
        Vec3 offset = GravityRotationUtil.vecPlayerToWorld(0.0D, BOLT_OFFSET, 0.0D, direction);
        return shooter.getEyePosition().subtract(offset).y + BOLT_OFFSET;
    }

    @WrapOperation(method = "shootProjectile",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D", ordinal = 0))
    private static double anthology$fixCrossbowSpawnZ(LivingEntity shooter, Operation<Double> original) {
        if (!anthology$shouldFixProjectileSpawn(shooter)) return original.call(shooter);
        GravityDirection direction = GravityApi.getGravityDirection(shooter);
        if (!direction.isInverted()) return original.call(shooter);
        Vec3 offset = GravityRotationUtil.vecPlayerToWorld(0.0D, BOLT_OFFSET, 0.0D, direction);
        return shooter.getEyePosition().subtract(offset).z;
    }

    private static boolean anthology$shouldFixProjectileSpawn(LivingEntity shooter) {
        return GravityHooks.shouldInvertInRegion(shooter);
    }
}
