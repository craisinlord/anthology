package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.craisinlord.anthology.time.TimeDilationEntityAccess;
import com.craisinlord.anthology.time.TimeDilationMath;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMoveTimeDilationMixin {
    @Inject(method = "move", at = @At("HEAD"))
    private void anthology$beginTimeDilationMove(MoverType moverType, Vec3 movement, CallbackInfo ci) {
        if (this instanceof TimeDilationEntityAccess access) {
            access.anthology$setInTimeDilationMove(true);
        }
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void anthology$endTimeDilationMove(MoverType moverType, Vec3 movement, CallbackInfo ci) {
        if (this instanceof TimeDilationEntityAccess access) {
            access.anthology$setInTimeDilationMove(false);
        }
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Vec3 anthology$scaleTimeDilatedMovement(Vec3 movement) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof Player && !entity.level().isClientSide()) {
            return movement;
        }
        double rate = TimeDilationApi.getRate(entity);
        if (Math.abs(rate - TimeDilationMath.NORMAL_RATE) < 0.001D) {
            return movement;
        }
        if (rate > TimeDilationMath.NORMAL_RATE && TimeDilationApi.isHorizontalOnlyMovement(entity)) {
            return movement.multiply(rate, 1.0D, rate);
        }
        return movement.scale(rate);
    }
}
