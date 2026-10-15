package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.craisinlord.anthology.time.TimeDilationManager;
import com.craisinlord.anthology.time.client.ContractionAfterimages;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SwingAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityTimeDilationMixin {
    @Inject(method = "tickEffects", at = @At("HEAD"))
    private void anthology$trackTemporalEffects(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        TimeDilationManager.trackPotentiallyAffected(entity);
        if (entity.level().isClientSide()) {
            ContractionAfterimages.observe((LivingEntity) entity);
        }
    }

    @WrapOperation(method = "baseTick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity$SwingState;tick()V"))
    private void anthology$slowSwingTime(LivingEntity.SwingState swingState, Operation<Void> original) {
        Entity entity = (Entity) (Object) this;
        if (TimeDilationApi.consumeTick(entity, "living_swing")) {
            original.call(swingState);
        }
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z", at = @At("HEAD"), cancellable = true)
    private void anthology$slowSwingStart(InteractionHand hand, SwingAnimation animation, boolean sendToSwingingEntity, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (!TimeDilationApi.consumeTick(entity, "living_swing_start")) {
            cir.setReturnValue(false);
        }
    }
}
