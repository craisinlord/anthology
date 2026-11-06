package com.craisinlord.anthology.mixins.compat.bettercombat;

import com.craisinlord.anthology.gravity.GravityApi;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.bettercombat.client.collision.TargetFinder")
@Pseudo
public abstract class BetterCombatTargetFinderGravityMixin {
    @WrapOperation(require = 0, method = "findAttackTargetResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getXRot()F"))
    private static float anthology$worldPitch(Player player, Operation<Float> original) {
        float pitch = original.call(player);
        return GravityApi.isGravityInverted(player) ? -pitch : pitch;
    }

    @WrapOperation(require = 0, method = "findAttackTargetResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"))
    private static float anthology$worldYaw(Player player, Operation<Float> original) {
        float yaw = original.call(player);
        return GravityApi.isGravityInverted(player) ? -yaw : yaw;
    }

    @Inject(require = 0, method = "getInitialTracingPoint", at = @At("RETURN"), cancellable = true)
    private static void anthology$mirrorShoulderOffset(Player player, CallbackInfoReturnable<Vec3> cir) {
        if (!GravityApi.isGravityInverted(player)) {
            return;
        }
        Vec3 eye = player.getEyePosition();
        cir.setReturnValue(eye.add(eye.subtract(cir.getReturnValue())));
    }
}
