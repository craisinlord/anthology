package com.craisinlord.anthology.mixins.compat.bettercombat;

import com.craisinlord.anthology.gravity.GravityApi;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.bettercombat.client.particle.SlashParticleUtil")
@Pseudo
public abstract class BetterCombatSlashParticleGravityMixin {
    @Unique
    private static final float anthology$MIRRORED_EYE_DROP = 0.5F;

    @Unique
    private static final float anthology$HALF_TURN_DEGREES = 180.0F;

    @WrapOperation(require = 0, method = "spawnParticles*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getXRot()F"))
    private static float anthology$worldPitch(AbstractClientPlayer player, Operation<Float> original) {
        float pitch = original.call(player);
        return GravityApi.isGravityInverted(player) ? -pitch : pitch;
    }

    @WrapOperation(require = 0, method = "spawnParticles*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getYRot()F"))
    private static float anthology$worldYaw(AbstractClientPlayer player, Operation<Float> original) {
        float yaw = original.call(player);
        return GravityApi.isGravityInverted(player) ? -yaw : yaw;
    }

    @ModifyExpressionValue(require = 0, method = "spawnParticles*", at = @At(value = "INVOKE", target = "Lnet/bettercombat/api/fx/ParticlePlacement;pitch_addition()F"))
    private static float anthology$worldPitchAddition(float pitchAddition, @Local(argsOnly = true) AbstractClientPlayer player) {
        return GravityApi.isGravityInverted(player) ? -pitchAddition : pitchAddition;
    }

    @ModifyExpressionValue(require = 0, method = "spawnParticles*", at = @At(value = "INVOKE", target = "Lnet/bettercombat/api/fx/ParticlePlacement;roll_set()F"))
    private static float anthology$worldRoll(float roll, @Local(argsOnly = true) AbstractClientPlayer player) {
        return GravityApi.isGravityInverted(player) ? roll + anthology$HALF_TURN_DEGREES : roll;
    }

    @ModifyExpressionValue(require = 0, method = "spawnParticles*", at = @At(value = "INVOKE", target = "Lnet/bettercombat/api/fx/ParticlePlacement;x_addition()F"))
    private static float anthology$worldSideOffset(float offset, @Local(argsOnly = true) AbstractClientPlayer player) {
        return GravityApi.isGravityInverted(player) ? -offset : offset;
    }

    @ModifyExpressionValue(require = 0, method = "spawnParticles*", at = @At(value = "INVOKE", target = "Lnet/bettercombat/api/fx/ParticlePlacement;y_addition()F"))
    private static float anthology$worldHeightOffset(float offset, @Local(argsOnly = true) AbstractClientPlayer player) {
        return GravityApi.isGravityInverted(player) ? anthology$MIRRORED_EYE_DROP - offset : offset;
    }
}
