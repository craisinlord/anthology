package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.craisinlord.anthology.time.TimeDilationMath;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class EntityRotationTimeDilationMixin {
    @ModifyVariable(method = "turn(DD)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double anthology$slowTurnYaw(double horizontal) {
        return this.anthology$scaleTurnInput(horizontal);
    }

    @ModifyVariable(method = "turn(DD)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double anthology$slowTurnPitch(double vertical) {
        return this.anthology$scaleTurnInput(vertical);
    }

    @Unique
    private double anthology$scaleTurnInput(double input) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof Player) || !entity.level().isClientSide() || TimeDilationApi.isRotationBypassed()) {
            return input;
        }
        double rate = TimeDilationApi.getRate(entity);
        return rate < TimeDilationMath.NORMAL_RATE ? input * rate : input;
    }

    @ModifyVariable(method = "setYRot", at = @At("HEAD"), argsOnly = true)
    private float anthology$slowYaw(float nextYaw) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof Player || TimeDilationApi.isRotationBypassed()) {
            return nextYaw;
        }
        double rate = TimeDilationApi.getRate(entity);
        if (rate >= TimeDilationMath.NORMAL_RATE) {
            return nextYaw;
        }
        return entity.getYRot() + Mth.wrapDegrees(nextYaw - entity.getYRot()) * (float) rate;
    }

    @ModifyVariable(method = "setXRot", at = @At("HEAD"), argsOnly = true)
    private float anthology$slowPitch(float nextPitch) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof Player || TimeDilationApi.isRotationBypassed()) {
            return nextPitch;
        }
        double rate = TimeDilationApi.getRate(entity);
        if (rate >= TimeDilationMath.NORMAL_RATE) {
            return nextPitch;
        }
        return entity.getXRot() + (nextPitch - entity.getXRot()) * (float) rate;
    }
}
