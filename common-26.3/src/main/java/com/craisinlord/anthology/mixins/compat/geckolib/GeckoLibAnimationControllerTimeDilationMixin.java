package com.craisinlord.anthology.mixins.compat.geckolib;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animation.AnimationController;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnimationController.class)
public abstract class GeckoLibAnimationControllerTimeDilationMixin {
    @Shadow(remap = false)
    protected double lastAnimatableAge;

    @ModifyExpressionValue(
            method = "checkControllerState",
            at = @At(value = "INVOKE", target = "Lcom/geckolib/animation/AnimationController;safetyCheckTickLinearity(DD)D"),
            remap = false
    )
    private double anthology$scaleAnimationTick(double age, @Local(argsOnly = true) GeoAnimatable animatable) {
        if (animatable instanceof Entity entity) {
            return this.lastAnimatableAge + (age - this.lastAnimatableAge) * TimeDilationApi.getRate(entity);
        }
        return age;
    }
}
