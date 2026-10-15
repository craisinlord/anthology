package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.craisinlord.anthology.time.TimeDilationEntityAccess;
import com.craisinlord.anthology.time.TimeDilationMath;
import com.craisinlord.anthology.time.TimeDilationTickScaled;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.craisinlord.anthology.portal.PortalEntity;
import com.craisinlord.anthology.portal.PortalRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile")
@Pseudo
public abstract class IronsSpellbooksMagicProjectileTimeDilationMixin {
    @Unique
    private Vec3 anthology$unscaledMotion;
    @Unique
    private Vec3 anthology$scaledMotion;
    @Unique
    private boolean anthology$wasInHomingMove;

    @Unique
    private double anthology$motionRate() {
        Entity self = (Entity) (Object) this;
        if (self instanceof TimeDilationTickScaled) {
            return TimeDilationMath.NORMAL_RATE;
        }
        return Math.max(TimeDilationMath.MIN_RATE, TimeDilationApi.getRate(self));
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lio/redspace/ironsspellbooks/entity/spells/AbstractMagicProjectile;tickCount:I", ordinal = 1))
    private int anthology$dilatedLifetime(int tickCount) {
        Entity self = (Entity) (Object) this;
        return self instanceof TimeDilationTickScaled ? tickCount : TimeDilationApi.getDilatedTickCount(self);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/entity/spells/AbstractMagicProjectile;handleHitDetection()V"), cancellable = true)
    private void anthology$scaleHitDetectionMotion(CallbackInfo ci) {
        this.anthology$unscaledMotion = null;
        this.anthology$scaledMotion = null;
        double rate = this.anthology$motionRate();
        if (this.anthology$passThroughPortal(rate)) {
            ci.cancel();
            return;
        }
        if (Math.abs(rate - TimeDilationMath.NORMAL_RATE) < 0.001D) {
            return;
        }
        Entity self = (Entity) (Object) this;
        this.anthology$unscaledMotion = self.getDeltaMovement();
        this.anthology$scaledMotion = this.anthology$unscaledMotion.scale(rate);
        anthology$setRawDeltaMovement(self, this.anthology$scaledMotion);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/entity/spells/AbstractMagicProjectile;handleHitDetection()V", shift = At.Shift.AFTER))
    private void anthology$restoreHitDetectionMotion(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (this.anthology$scaledMotion != null && self.getDeltaMovement() == this.anthology$scaledMotion) {
            anthology$setRawDeltaMovement(self, this.anthology$unscaledMotion);
        }
        this.anthology$unscaledMotion = null;
        this.anthology$scaledMotion = null;
    }

    @ModifyArg(method = "travel", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/entity/spells/AbstractMagicProjectile;setPos(Lnet/minecraft/world/phys/Vec3;)V"))
    private Vec3 anthology$scaleTravel(Vec3 position) {
        double rate = this.anthology$motionRate();
        if (Math.abs(rate - TimeDilationMath.NORMAL_RATE) < 0.001D) {
            return position;
        }
        Vec3 current = ((Entity) (Object) this).position();
        return current.add(position.subtract(current).scale(rate));
    }

    @ModifyArg(method = "homeTowards", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/api/util/Utils;slerp(DLnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"), index = 0)
    private double anthology$scaleHomingStrength(double strength) {
        double rate = this.anthology$motionRate();
        if (Math.abs(rate - TimeDilationMath.NORMAL_RATE) < 0.001D || strength <= 0.0D || strength >= 1.0D) {
            return strength;
        }
        return 1.0D - Math.pow(1.0D - strength, rate);
    }

    @Inject(method = "homeTowards", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/entity/spells/AbstractMagicProjectile;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void anthology$beginHomingMove(CallbackInfoReturnable<Vec3> cir) {
        if ((Object) this instanceof TimeDilationEntityAccess access) {
            this.anthology$wasInHomingMove = access.anthology$isInTimeDilationMove();
            access.anthology$setInTimeDilationMove(true);
        }
    }

    @Inject(method = "homeTowards", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/entity/spells/AbstractMagicProjectile;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", shift = At.Shift.AFTER))
    private void anthology$endHomingMove(CallbackInfoReturnable<Vec3> cir) {
        if ((Object) this instanceof TimeDilationEntityAccess access) {
            access.anthology$setInTimeDilationMove(this.anthology$wasInHomingMove);
        }
    }

    @Unique
    private boolean anthology$passThroughPortal(double rate) {
        Entity self = (Entity) (Object) this;
        if (!PortalRegistry.hasAny(self.level())) {
            return false;
        }
        Vec3 position = self.position();
        Vec3 end = position.add(self.getDeltaMovement().scale(rate));
        BlockHitResult hit = self.level().clip(new ClipContext(position, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
        return hit.getType() == HitResult.Type.BLOCK && PortalEntity.passProjectileThroughPortal((Projectile) self, hit);
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
