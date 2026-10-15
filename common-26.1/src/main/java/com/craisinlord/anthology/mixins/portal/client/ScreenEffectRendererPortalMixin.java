package com.craisinlord.anthology.mixins.portal.client;

import com.craisinlord.anthology.portal.client.PortalRollClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererPortalMixin {
    @Inject(method = "getViewBlockingState", at = @At("HEAD"), cancellable = true)
    private static void anthology$ignorePortalViewBlock(Player player, CallbackInfoReturnable<net.minecraft.world.level.block.state.BlockState> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player != null && PortalRollClientState.isEntityInsidePortal(minecraft, player)) {
            cir.setReturnValue(null);
        }
    }
}
