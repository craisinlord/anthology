package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.effect.AnthologyEffects;
import com.craisinlord.anthology.effect.ContractedMobEffect;
import com.craisinlord.anthology.effect.DilatedMobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class TemporalMiningSpeedMixin {
    @Unique
    private boolean anthology$miningSpeedAppliedInner;

    @Inject(method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)F",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void anthology$applyTemporalMiningSpeedWithPos(CallbackInfoReturnable<Float> cir) {
        this.anthology$applyTemporalMiningSpeed(cir);
        this.anthology$miningSpeedAppliedInner = true;
    }

    @Inject(method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F", at = @At("HEAD"))
    private void anthology$resetMiningSpeedGuard(CallbackInfoReturnable<Float> cir) {
        this.anthology$miningSpeedAppliedInner = false;
    }

    @Inject(method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F", at = @At("RETURN"), cancellable = true)
    private void anthology$applyTemporalMiningSpeedNoPos(CallbackInfoReturnable<Float> cir) {
        if (this.anthology$miningSpeedAppliedInner) {
            this.anthology$miningSpeedAppliedInner = false;
            return;
        }
        this.anthology$applyTemporalMiningSpeed(cir);
    }

    @Unique
    private void anthology$applyTemporalMiningSpeed(CallbackInfoReturnable<Float> cir) {
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
