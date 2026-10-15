package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.craisinlord.anthology.time.TimeDilationMath;
import com.craisinlord.anthology.time.client.ClientTimeDilationTicker;
import com.craisinlord.anthology.time.client.TimeDilationRenderState;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererAnimationTimeDilationMixin {
    private static final String EXTRACT = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V";

    @Redirect(method = EXTRACT, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;rotLerp(FFF)F", ordinal = 0))
    private float anthology$slowHeadYaw(float partialTick, float previous, float current, @Local(argsOnly = true) LivingEntity entity) {
        return ClientTimeDilationTicker.dilateRotation(entity, previous, current, partialTick);
    }

    @Redirect(method = "solveBodyRot", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;rotLerp(FFF)F", ordinal = 1))
    private static float anthology$slowBodyYaw(float partialTick, float previous, float current, @Local(argsOnly = true) LivingEntity entity) {
        return ClientTimeDilationTicker.dilateRotation(entity, previous, current, partialTick);
    }

    @WrapOperation(method = EXTRACT, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot(F)F"))
    private float anthology$slowHeadPitch(LivingEntity entity, float partialTick, Operation<Float> original) {
        if (TimeDilationApi.getRate(entity) >= TimeDilationMath.NORMAL_RATE) {
            return original.call(entity, partialTick);
        }
        return ClientTimeDilationTicker.dilatePitch(entity, entity.xRotO, entity.getXRot(), partialTick);
    }

    @WrapOperation(method = EXTRACT, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/WalkAnimationState;speed(F)F"))
    private float anthology$slowWalkAmount(WalkAnimationState walkAnimation, float partialTick, Operation<Float> original,
                                           @Local(argsOnly = true) LivingEntity entity) {
        return ClientTimeDilationTicker.dilateWalkAmount(entity, original.call(walkAnimation, partialTick));
    }

    @WrapOperation(method = EXTRACT, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/WalkAnimationState;position(F)F", ordinal = 0))
    private float anthology$scaleWalkPosition(WalkAnimationState walkAnimation, float partialTick, Operation<Float> original,
                                              @Local(argsOnly = true) LivingEntity entity) {
        return ClientTimeDilationTicker.dilateWalkPosition(entity, original.call(walkAnimation, partialTick));
    }

    @Inject(method = EXTRACT, at = @At("TAIL"))
    private void anthology$slowIdleAnimation(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
        state.ageInTicks = ClientTimeDilationTicker.dilateAnimationTime(entity, state.ageInTicks);
        ((TimeDilationRenderState) state).anthology$setTimeDilatedHurt(TimeDilationApi.getRate(entity) < TimeDilationMath.NORMAL_RATE
                && (entity.hurtTime > 0 || entity.deathTime > 0));
    }

    @ModifyReturnValue(method = "getOverlayCoords", at = @At("RETURN"))
    private static int anthology$slowHurtOverlay(int overlay, LivingEntityRenderState state, float whiteOverlayProgress) {
        if (((TimeDilationRenderState) state).anthology$isTimeDilatedHurt()) {
            return OverlayTexture.pack(1.0F, state.deathTime > 0.0F);
        }
        return overlay;
    }
}
