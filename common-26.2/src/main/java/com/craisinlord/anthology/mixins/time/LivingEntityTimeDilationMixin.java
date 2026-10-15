package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.craisinlord.anthology.time.TimeDilationManager;
import com.craisinlord.anthology.time.client.ContractionAfterimages;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Inject(method = "updateSwingTime", at = @At("HEAD"), cancellable = true)
    private void anthology$slowSwingTime(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (!TimeDilationApi.consumeTick(entity, "living_swing")) {
            ci.cancel();
        }
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"), cancellable = true)
    private void anthology$slowSwingStart(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (!TimeDilationApi.consumeTick(entity, "living_swing_start")) {
            ci.cancel();
        }
    }
}
