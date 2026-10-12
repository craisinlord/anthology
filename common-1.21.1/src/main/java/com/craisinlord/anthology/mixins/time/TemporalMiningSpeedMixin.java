package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.effect.AnthologyEffects;
import com.craisinlord.anthology.effect.ContractedMobEffect;
import com.craisinlord.anthology.effect.DilatedMobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class TemporalMiningSpeedMixin {
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void anthology$applyTemporalMiningSpeed(
            net.minecraft.world.level.block.state.BlockState state,
            CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        double rate = 1.0D;

        if (AnthologyEffects.dilated() != null) {
            MobEffectInstance dilation = player.getEffect(AnthologyEffects.dilated());
            if (dilation != null) {
                rate = Math.min(rate, DilatedMobEffect.rateForAmplifier(dilation.getAmplifier()));
            }
        }
        if (AnthologyEffects.contracted() != null) {
            MobEffectInstance contraction = player.getEffect(AnthologyEffects.contracted());
            if (contraction != null) {
                rate = Math.max(rate, ContractedMobEffect.rateForAmplifier(contraction.getAmplifier()));
            }
        }

        if (rate != 1.0D) {
            cir.setReturnValue((float) (cir.getReturnValue() * rate));
        }
    }
}
