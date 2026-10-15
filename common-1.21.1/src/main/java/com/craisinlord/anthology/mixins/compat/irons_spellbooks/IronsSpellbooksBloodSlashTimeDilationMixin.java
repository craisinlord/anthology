package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.craisinlord.anthology.time.TimeDilationEntityAccess;
import com.craisinlord.anthology.time.TimeDilationMath;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.function.Predicate;
import com.craisinlord.anthology.portal.PortalEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(targets = "io.redspace.ironsspellbooks.entity.spells.blood_slash.BloodSlashProjectile")
@Pseudo
public abstract class IronsSpellbooksBloodSlashTimeDilationMixin {
    @Unique
    private double anthology$motionRate() {
        return Math.max(TimeDilationMath.MIN_RATE, TimeDilationApi.getRate((Entity) (Object) this));
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lio/redspace/ironsspellbooks/entity/spells/blood_slash/BloodSlashProjectile;age:I", opcode = 180))
    private int anthology$dilateAge(int age) {
        return age + TimeDilationApi.consumeTicks((Entity) (Object) this, "irons_blood_slash_age") - 1;
    }

    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.12F))
    private float anthology$dilateGrowth(float growth) {
        return (float) (growth * this.anthology$motionRate());
    }

    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/entity/spells/blood_slash/BloodSlashProjectile;setPos(Lnet/minecraft/world/phys/Vec3;)V"))
    private Vec3 anthology$scaleTravel(Vec3 position) {
        double rate = this.anthology$motionRate();
        if (Math.abs(rate - TimeDilationMath.NORMAL_RATE) < 0.001D) {
            return position;
        }
        Vec3 current = ((Entity) (Object) this).position();
        return current.add(position.subtract(current).scale(rate));
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getHitResultOnMoveVector(Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;)Lnet/minecraft/world/phys/HitResult;"))
    private HitResult anthology$scaleHitDetection(Entity entity, Predicate<Entity> filter, Operation<HitResult> original) {
        double rate = this.anthology$motionRate();
        HitResult result;
        if (Math.abs(rate - TimeDilationMath.NORMAL_RATE) < 0.001D) {
            result = original.call(entity, filter);
        } else {
            Vec3 motion = entity.getDeltaMovement();
            anthology$setRawDeltaMovement(entity, motion.scale(rate));
            result = original.call(entity, filter);
            anthology$setRawDeltaMovement(entity, motion);
        }
        if (result instanceof BlockHitResult blockHit && result.getType() == HitResult.Type.BLOCK
                && PortalEntity.passProjectileThroughPortal((Projectile) entity, blockHit)) {
            return BlockHitResult.miss(entity.position(), blockHit.getDirection(), entity.blockPosition());
        }
        return result;
    }

    @Unique
    private static void anthology$setRawDeltaMovement(Entity entity, Vec3 motion) {
        if (entity instanceof TimeDilationEntityAccess access) {
            boolean wasInMove = access.anthology$isInTimeDilationMove();
            access.anthology$setInTimeDilationMove(true);
            entity.setDeltaMovement(motion);
            access.anthology$setInTimeDilationMove(wasInMove);
        } else {
            entity.setDeltaMovement(motion);
        }
    }
}
