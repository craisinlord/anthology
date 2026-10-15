package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityHooks;
import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityDirection;
import com.craisinlord.anthology.gravity.GravityRotationUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @WrapOperation(method = "travel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getY()D"))
    private double anthology$fixTravelGetY(LivingEntity entity, Operation<Double> original) {
        if (!GravityApi.isGravityInverted(entity)) return original.call(entity);
        GravityDirection direction = GravityApi.getGravityDirection(entity);
        return GravityRotationUtil.vecWorldToPlayer(entity.position(), direction).y;
    }

    @WrapOperation(method = "travel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0))
    private Vec3 anthology$fixTravelLookAngle(LivingEntity entity, Operation<Vec3> original) {
        Vec3 vec3 = original.call(entity);
        GravityDirection direction = GravityApi.getGravityDirection(entity);
        if (!direction.isInverted()) return vec3;
        Vec3 localLook = GravityRotationUtil.vecWorldToPlayer(vec3, direction);
        if (!entity.isFallFlying()) {
            return localLook;
        }

        return new Vec3(-localLook.x, localLook.y, localLook.z);
    }

    @ModifyVariable(method = "checkFallDamage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double anthology$invertLivingFallDamageY(double worldY) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!GravityApi.isGravityInverted(entity)) {
            return worldY;
        }
        if (anthology$isTouchingAntiwater(entity)) {
            entity.fallDistance = 0.0F;
            return 0.0D;
        }
        return -worldY;
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "calculateEntityAnimation", at = @At("HEAD"), cancellable = true)
    private void anthology$fixEntityAnimation(boolean flutter, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!GravityApi.isGravityInverted(entity)) return;
        ci.cancel();

        GravityDirection direction = GravityApi.getGravityDirection(entity);
        Vec3 playerPosDelta = GravityRotationUtil.vecWorldToPlayer(
                new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo),
                direction
        );
        double d = playerPosDelta.x;
        double e = flutter ? playerPosDelta.y : 0.0D;
        double f = playerPosDelta.z;
        float g = (float) Math.sqrt(d * d + e * e + f * f) * 4.0F;
        if (g > 1.0F) g = 1.0F;
        entity.walkAnimation.update(g, 0.4F);
    }


    @Unique
    private boolean anthology$isTouchingAntiwater(LivingEntity entity) {
        AABB box = entity.getBoundingBox().inflate(0.05D);
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    cursor.set(x, y, z);
                    FluidState fluidState = entity.level().getFluidState(cursor);
                    if (GravityHooks.isInvertedFluid(fluidState)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "getLocalBoundsForPose", at = @At("RETURN"), cancellable = true)
    private void anthology$fixLocalBoundsForPose(Pose pose, CallbackInfoReturnable<AABB> cir) {
        GravityDirection direction = GravityApi.getGravityDirection((Entity) (Object) this);
        if (!direction.isInverted()) return;
        AABB box = cir.getReturnValue();
        box = box.move(0.0D, -1.0E-6D, 0.0D);
        cir.setReturnValue(GravityRotationUtil.boxPlayerToWorld(box, direction));
    }

    @WrapOperation(method = "hasLineOfSight",
            at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/world/phys/Vec3;", ordinal = 0))
    private Vec3 anthology$fixHasLineOfSightSelf(double x, double y, double z, Operation<Vec3> original) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!GravityApi.isGravityInverted(self)) return original.call(x, y, z);
        return self.getEyePosition();
    }

    @WrapOperation(method = "hasLineOfSight",
            at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/world/phys/Vec3;", ordinal = 1))
    private Vec3 anthology$fixHasLineOfSightTarget(double x, double y, double z, Operation<Vec3> original,
            @Local(argsOnly = true) Entity other) {
        if (!GravityApi.isGravityInverted(other)) return original.call(x, y, z);
        return other.getEyePosition();
    }

    @WrapOperation(method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D", ordinal = 0))
    private double anthology$fixTickBodyRotGetX(LivingEntity entity, Operation<Double> original) {
        GravityDirection direction = GravityApi.getGravityDirection(entity);
        if (!direction.isInverted()) return original.call(entity);
        return GravityRotationUtil.vecWorldToPlayer(
                original.call(entity) - entity.xo,
                entity.getY() - entity.yo,
                entity.getZ() - entity.zo,
                direction
        ).x + entity.xo;
    }

    @WrapOperation(method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D", ordinal = 0))
    private double anthology$fixTickBodyRotGetZ(LivingEntity entity, Operation<Double> original) {
        GravityDirection direction = GravityApi.getGravityDirection(entity);
        if (!direction.isInverted()) return original.call(entity);
        return GravityRotationUtil.vecWorldToPlayer(
                entity.getX() - entity.xo,
                entity.getY() - entity.yo,
                original.call(entity) - entity.zo,
                direction
        ).z + entity.zo;
    }

    @WrapOperation(method = "blockedByShield",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D", ordinal = 0))
    private double anthology$fixShieldAttackerX(LivingEntity attacker, Operation<Double> original) {
        GravityDirection defDir = GravityApi.getGravityDirection((Entity) (Object) this);
        if (!defDir.isInverted()) {
            if (GravityApi.isGravityInverted(attacker)) return attacker.getEyePosition().x;
            return original.call(attacker);
        }
        return GravityRotationUtil.vecWorldToPlayer(attacker.getEyePosition(), defDir).x;
    }

    @WrapOperation(method = "blockedByShield",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D", ordinal = 0))
    private double anthology$fixShieldAttackerZ(LivingEntity attacker, Operation<Double> original) {
        GravityDirection defDir = GravityApi.getGravityDirection((Entity) (Object) this);
        if (!defDir.isInverted()) return original.call(attacker);
        return GravityRotationUtil.vecWorldToPlayer(attacker.getEyePosition(), defDir).z;
    }

    @WrapOperation(method = "blockedByShield",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D", ordinal = 1))
    private double anthology$fixShieldDefenderX(LivingEntity shieldUser, Operation<Double> original) {
        GravityDirection direction = GravityApi.getGravityDirection((Entity) (Object) this);
        if (!direction.isInverted()) return original.call(shieldUser);
        return GravityRotationUtil.vecWorldToPlayer(shieldUser.position(), direction).x;
    }

    @WrapOperation(method = "blockedByShield",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D", ordinal = 1))
    private double anthology$fixShieldDefenderZ(LivingEntity shieldUser, Operation<Double> original) {
        GravityDirection direction = GravityApi.getGravityDirection((Entity) (Object) this);
        if (!direction.isInverted()) return original.call(shieldUser);
        return GravityRotationUtil.vecWorldToPlayer(shieldUser.position(), direction).z;
    }

    @WrapOperation(method = "isDamageSourceBlocked",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getViewVector(F)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0))
    private Vec3 anthology$fixShieldArcViewVector(LivingEntity entity, float partialTick, Operation<Vec3> original) {
        GravityDirection direction = GravityApi.getGravityDirection(entity);
        if (!direction.isInverted()) return original.call(entity, partialTick);
        return GravityRotationUtil.vecWorldToPlayer(Vec3.directionFromRotation(0.0F, entity.getYHeadRot()), direction);
    }


    @ModifyArg(method = "playBlockFallSound",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"),
            index = 0)
    private BlockPos anthology$fixBlockFallSoundPos(BlockPos pos) {
        GravityDirection direction = GravityApi.getGravityDirection((Entity) (Object) this);
        if (!direction.isInverted()) return pos;
        Entity selfEntity = (Entity) (Object) this;
        return BlockPos.containing(selfEntity.position().add(
                GravityRotationUtil.vecPlayerToWorld(new Vec3(0.0D, -0.20000000298023224D, 0.0D), direction)
        ));
    }

    @WrapOperation(method = "spawnItemParticles",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0))
    private Vec3 anthology$fixItemParticlesAdd(Vec3 base, double x, double y, double z, Operation<Vec3> original) {
        Entity self = (Entity) (Object) this;
        GravityDirection direction = GravityApi.getGravityDirection(self);
        if (!direction.isInverted()) return original.call(base, x, y, z);
        Vec3 rotated = GravityRotationUtil.vecPlayerToWorld(new Vec3(x, y, z), direction);
        return original.call(self.getEyePosition(), rotated.x, rotated.y, rotated.z);
    }

    @ModifyVariable(method = "spawnItemParticles",
            at = @At(value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/phys/Vec3;yRot(F)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0),
            ordinal = 0)
    private Vec3 anthology$fixItemParticlesYRotVec(Vec3 vec3) {
        GravityDirection direction = GravityApi.getGravityDirection((Entity) (Object) this);
        if (!direction.isInverted()) return vec3;
        return GravityRotationUtil.vecPlayerToWorld(vec3, direction);
    }

    @WrapOperation(method = "tickEffects",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void anthology$fixEffectParticles(Level level, ParticleOptions particle,
            double x, double y, double z, double dx, double dy, double dz,
            Operation<Void> original) {
        Entity self = (Entity) (Object) this;
        GravityDirection direction = GravityApi.getGravityDirection(self);
        if (!direction.isInverted()) {
            original.call(level, particle, x, y, z, dx, dy, dz);
            return;
        }
        Vec3 pos = self.position().subtract(
                GravityRotationUtil.vecPlayerToWorld(self.position().subtract(x, y, z), direction)
        );
        original.call(level, particle, pos.x, pos.y, pos.z, dx, dy, dz);
    }

    @WrapOperation(method = "makePoofParticles",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
                    ordinal = 0))
    private void anthology$fixPoofParticles(Level level, ParticleOptions particle,
            double x, double y, double z, double dx, double dy, double dz,
            Operation<Void> original) {
        Entity self = (Entity) (Object) this;
        GravityDirection direction = GravityApi.getGravityDirection(self);
        if (!direction.isInverted()) {
            original.call(level, particle, x, y, z, dx, dy, dz);
            return;
        }
        Vec3 pos = self.position().subtract(
                GravityRotationUtil.vecPlayerToWorld(self.position().subtract(x, y, z), direction)
        );
        original.call(level, particle, pos.x, pos.y, pos.z, dx, dy, dz);
    }
}
