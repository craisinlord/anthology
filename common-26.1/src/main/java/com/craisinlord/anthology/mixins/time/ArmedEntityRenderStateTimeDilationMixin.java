package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.client.ClientTimeDilationTicker;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ArmedEntityRenderState.class)
public abstract class ArmedEntityRenderStateTimeDilationMixin {
    @ModifyExpressionValue(method = "extractArmedEntityRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getAttackAnim(F)F"))
    private static float anthology$slowAttackAnimation(float animation, @Local(argsOnly = true) LivingEntity entity,
                                                       @Local(argsOnly = true) float partialTicks) {
        return ClientTimeDilationTicker.dilateAttackAnimation(entity, partialTicks, animation);
    }
}
