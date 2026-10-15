package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityDirection;
import com.craisinlord.anthology.gravity.GravityRotationUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerGravityMixin {
    @Redirect(
            method = "travel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 anthology$rotateTravelLookAngle(Player player) {
        Vec3 lookAngle = player.getLookAngle();
        GravityDirection direction = GravityApi.getGravityDirection(player);
        if (!direction.isInverted() || player.isFallFlying()) return lookAngle;
        return GravityRotationUtil.vecWorldToPlayer(lookAngle, direction);
    }

    @WrapOperation(method = "travel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;containing(DDD)Lnet/minecraft/core/BlockPos;"))
    private BlockPos anthology$fixTravelBlockPos(double x, double y, double z,
            Operation<BlockPos> original) {
        GravityDirection direction = GravityApi.getGravityDirection((Entity) (Object) this);
        if (!direction.isInverted()) return original.call(x, y, z);
        Vec3 rotate = GravityRotationUtil.vecPlayerToWorld(
                new Vec3(0.0D, 0.9D, 0.0D), direction);
        return original.call(x - rotate.x, y - rotate.y + 0.9D, z - rotate.z);
    }

    @WrapOperation(method = "causeExtraKnockback",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getYRot()F", ordinal = 0))
    private float anthology$fixAttackYaw0(Player attacker, Operation<Float> original,
            @Local(argsOnly = true) Entity target) {
        GravityDirection targetDir = GravityApi.getGravityDirection(target);
        GravityDirection attackerDir = GravityApi.getGravityDirection(attacker);
        if (targetDir == attackerDir) return original.call(attacker);
        Vec2 worldRot = GravityRotationUtil.rotPlayerToWorld(
                original.call(attacker), attacker.getXRot(), attackerDir);
        return GravityRotationUtil.rotWorldToPlayer(worldRot.x, worldRot.y, targetDir).x;
    }

    @WrapOperation(method = "causeExtraKnockback",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getYRot()F", ordinal = 1))
    private float anthology$fixAttackYaw1(Player attacker, Operation<Float> original,
            @Local(argsOnly = true) Entity target) {
        GravityDirection targetDir = GravityApi.getGravityDirection(target);
        GravityDirection attackerDir = GravityApi.getGravityDirection(attacker);
        if (targetDir == attackerDir) return original.call(attacker);
        Vec2 worldRot = GravityRotationUtil.rotPlayerToWorld(
                original.call(attacker), attacker.getXRot(), attackerDir);
        return GravityRotationUtil.rotWorldToPlayer(worldRot.x, worldRot.y, targetDir).x;
    }

    @WrapOperation(method = "causeExtraKnockback",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getYRot()F", ordinal = 2))
    private float anthology$fixAttackYaw2(Player attacker, Operation<Float> original) {
        GravityDirection direction = GravityApi.getGravityDirection(attacker);
        if (!direction.isInverted()) return original.call(attacker);
        return GravityRotationUtil.rotPlayerToWorld(
                original.call(attacker), attacker.getXRot(), direction).x;
    }

    @WrapOperation(method = "causeExtraKnockback",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getYRot()F", ordinal = 3))
    private float anthology$fixAttackYaw3(Player attacker, Operation<Float> original) {
        GravityDirection direction = GravityApi.getGravityDirection(attacker);
        if (!direction.isInverted()) return original.call(attacker);
        return GravityRotationUtil.rotPlayerToWorld(
                original.call(attacker), attacker.getXRot(), direction).x;
    }
}
