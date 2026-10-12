package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.craisinlord.anthology.time.TimeDilationMath;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
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

    @ModifyReturnValue(method = "rotateTowards", at = @At("RETURN"))
    private float anthology$slowLookTurn(float result, float from, float to, float maxAngle) {
        double rate = TimeDilationApi.getRate(this.mob);
        if (rate >= TimeDilationMath.NORMAL_RATE) {
            return result;
        }
        return from + (float) (Mth.wrapDegrees(result - from) * rate);
    }
}
