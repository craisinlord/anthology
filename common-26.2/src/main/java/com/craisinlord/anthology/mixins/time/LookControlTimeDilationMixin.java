package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.craisinlord.anthology.time.TimeDilationMath;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LookControl.class)
public abstract class LookControlTimeDilationMixin {
    @Shadow
    protected Mob mob;

    @WrapOperation(
            method = {"tick", "lambda$tick$0", "lambda$tick$1"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/control/LookControl;rotateTowards(FFF)F")
    )
    private float anthology$slowLookTurn(LookControl control, float from, float to, float maxAngle, Operation<Float> original) {
        float result = original.call(control, from, to, maxAngle);
        double rate = TimeDilationApi.getRate(this.mob);
        if (rate >= TimeDilationMath.NORMAL_RATE) {
            return result;
        }
        return from + (float) (Mth.wrapDegrees(result - from) * rate);
    }
}
