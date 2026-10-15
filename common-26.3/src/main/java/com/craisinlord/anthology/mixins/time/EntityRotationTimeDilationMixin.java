package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.craisinlord.anthology.time.TimeDilationMath;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class EntityRotationTimeDilationMixin {
    @WrapMethod(method = "turn(DD)V")
    private void anthology$slowTurn(double horizontal, double vertical, Operation<Void> original) {
        Entity entity = (Entity) (Object) this;
        double rate = TimeDilationApi.getRate(entity);
        if (TimeDilationApi.isRotationBypassed() || rate >= TimeDilationMath.NORMAL_RATE) {
            original.call(horizontal, vertical);
            return;
        }
        TimeDilationApi.enterRotationBypass();
        try {
            original.call(horizontal * rate, vertical * rate);
        } finally {
            TimeDilationApi.exitRotationBypass();
        }
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
