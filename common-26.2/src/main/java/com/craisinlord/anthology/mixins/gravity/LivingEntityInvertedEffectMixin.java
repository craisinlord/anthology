package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.effect.AnthologyEffects;
import com.craisinlord.anthology.gravity.GravityAccess;
import com.craisinlord.anthology.gravity.MountedGravityHelper;
import java.util.Collection;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityInvertedEffectMixin {
    @Inject(method = "onEffectAdded", at = @At("TAIL"))
    private void anthology$applyInvertedGravity(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        if (effect.getEffect().equals(AnthologyEffects.inverted())) {
            MountedGravityHelper.syncConnectedStack((LivingEntity) (Object) this);
        }
    }

    @Inject(method = "onEffectUpdated", at = @At("TAIL"))
    private void anthology$refreshInvertedGravity(MobEffectInstance effect, boolean refreshAttributes, Entity source, CallbackInfo ci) {
        if (effect.getEffect().equals(AnthologyEffects.inverted())) {
            MountedGravityHelper.syncConnectedStack((LivingEntity) (Object) this);
        }
    }

    @Inject(method = "tickEffects", at = @At("TAIL"))
    private void anthology$reconcileInvertedGravity(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (((GravityAccess) entity).anthology$isEffectGravityManaged()) {
            MountedGravityHelper.syncConnectedStack(entity);
        }
    }
    @Inject(method = "onEffectsRemoved", at = @At("TAIL"))
    private void anthology$removeInvertedGravity(Collection<MobEffectInstance> effects, CallbackInfo ci) {
        for (MobEffectInstance effect : effects) {
            if (effect.getEffect().equals(AnthologyEffects.inverted())) {
                MountedGravityHelper.syncConnectedStack((LivingEntity) (Object) this);
                return;
            }
        }
    }
}
