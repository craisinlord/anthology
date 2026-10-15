package com.craisinlord.anthology.mixins.portal.client;

import com.craisinlord.anthology.portal.client.PortalRollClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerPortalMixin {
    @Inject(method = "suffocatesAt", at = @At("HEAD"), cancellable = true)
    private void anthology$ignorePortalSuffocation(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && PortalRollClientState.isEntityInsidePortal(minecraft, minecraft.player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void anthology$cancelPortalPushOut(double x, double z, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && PortalRollClientState.isEntityInsidePortal(minecraft, minecraft.player)) {
            ci.cancel();
        }
    }
}
