package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.craisinlord.anthology.time.TimeDilationEntityAccess;
import com.craisinlord.anthology.time.TimeDilationMath;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class EntityDeltaMovementTimeDilationMixin {
    @ModifyVariable(method = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Vec3 anthology$scaleTimeDilatedVelocityChange(Vec3 nextMovement) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof TimeDilationEntityAccess access)
                || access.anthology$isInTimeDilationMove()
                || access.anthology$isApplyingExternalImpulse()) {
            return nextMovement;
        }

        double rate = TimeDilationApi.getRate(entity);
        if (rate >= TimeDilationMath.NORMAL_RATE - 0.001D) {
            return nextMovement;
        }

        Vec3 currentMovement = entity.getDeltaMovement();
        double y = currentMovement.y + (nextMovement.y - currentMovement.y) * rate;
        if (nextMovement.y > currentMovement.y && nextMovement.y > 0.0D && (entity.onGround() || currentMovement.y <= 0.0D)) {
            y = nextMovement.y;
        }
        return new Vec3(
                currentMovement.x + (nextMovement.x - currentMovement.x) * rate,
                y,
                currentMovement.z + (nextMovement.z - currentMovement.z) * rate
        );
    }
}
