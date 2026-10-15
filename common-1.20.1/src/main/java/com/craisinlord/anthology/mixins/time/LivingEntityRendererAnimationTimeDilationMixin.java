package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.client.ClientTimeDilationTicker;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererAnimationTimeDilationMixin {
    private static final ThreadLocal<LivingEntity> anthology$renderingEntity = new ThreadLocal<>();

    @org.spongepowered.asm.mixin.injection.Inject(method = "render", at = @At("HEAD"))
    private void anthology$beginRender(LivingEntity entity, float entityYaw, float partialTick,
                                       com.mojang.blaze3d.vertex.PoseStack poseStack,
                                       net.minecraft.client.renderer.MultiBufferSource buffer,
                                       int packedLight, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        anthology$renderingEntity.set(entity);
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "render", at = @At("RETURN"))
    private void anthology$endRender(LivingEntity entity, float entityYaw, float partialTick,
                                     com.mojang.blaze3d.vertex.PoseStack poseStack,
                                     net.minecraft.client.renderer.MultiBufferSource buffer,
                                     int packedLight, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        anthology$renderingEntity.remove();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;rotLerp(FFF)F", ordinal = 0))
    private float anthology$slowBodyYaw(float partialTick, float previous, float current) {
        LivingEntity entity = anthology$renderingEntity.get();
        return entity == null ? Mth.rotLerp(partialTick, previous, current)
                : ClientTimeDilationTicker.dilateRotation(entity, previous, current, partialTick);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;rotLerp(FFF)F", ordinal = 1))
    private float anthology$slowHeadYaw(float partialTick, float previous, float current) {
        LivingEntity entity = anthology$renderingEntity.get();
        return entity == null ? Mth.rotLerp(partialTick, previous, current)
                : ClientTimeDilationTicker.dilateRotation(entity, previous, current, partialTick);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F", ordinal = 0))
    private float anthology$slowHeadPitch(float partialTick, float previous, float current) {
        LivingEntity entity = anthology$renderingEntity.get();
        return entity == null ? Mth.lerp(partialTick, previous, current)
                : ClientTimeDilationTicker.dilatePitch(entity, previous, current, partialTick);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/WalkAnimationState;speed(F)F"))
    private float anthology$slowWalkAmount(net.minecraft.world.entity.WalkAnimationState state, float partialTick) {
        LivingEntity entity = anthology$renderingEntity.get();
        float amount = state.speed(partialTick);
        return entity == null ? amount : ClientTimeDilationTicker.dilateWalkAmount(entity, amount);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/WalkAnimationState;position(F)F"))
    private float anthology$scaleWalkPosition(net.minecraft.world.entity.WalkAnimationState state, float partialTick) {
        LivingEntity entity = anthology$renderingEntity.get();
        float position = state.position(partialTick);
        return entity == null ? position : ClientTimeDilationTicker.dilateWalkPosition(entity, position);
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true)
    private static void anthology$slowHurtOverlay(LivingEntity entity, float partialTick, CallbackInfoReturnable<Integer> cir) {
        if (com.craisinlord.anthology.time.TimeDilationApi.getRate(entity) < 1.0D
                && (entity.hurtTime > 0 || entity.deathTime > 0)) {
            cir.setReturnValue(net.minecraft.client.renderer.texture.OverlayTexture.pack(1.0F, entity.deathTime > 0));
        }
    }
    @ModifyReturnValue(method = "getAttackAnim", at = @At("RETURN"))
    private float anthology$slowAttackAnimation(float animation, LivingEntity entity, float partialTick) {
        return ClientTimeDilationTicker.dilateAttackAnimation(entity, partialTick, animation);
    }

    @ModifyReturnValue(method = "getBob", at = @At("RETURN"))
    private float anthology$slowIdleAnimation(float bob, LivingEntity entity, float partialTick) {
        return ClientTimeDilationTicker.dilateAnimationTime(entity, bob);
    }
}
