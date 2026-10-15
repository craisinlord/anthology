package com.craisinlord.anthology.mixins.portal.client;

import com.craisinlord.anthology.portal.client.PortalRollClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorPortalViewBlockingMixin {
    @Inject(method = "getViewBlockingState", at = @At("HEAD"), cancellable = true)
    private static void anthology$ignorePortalViewBlock(LocalPlayer player, Frustum frustum, CallbackInfoReturnable<BlockState> cir) {
        if (anthology$isInsidePortal(player)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "getViewBlockingStateAndPos", at = @At("HEAD"), cancellable = true, require = 0)
    private static void anthology$ignorePortalViewBlockAndPos(LocalPlayer player, Frustum frustum, CallbackInfoReturnable<Pair<BlockState, BlockPos>> cir) {
        if (anthology$isInsidePortal(player)) {
            cir.setReturnValue(null);
        }
    }

    @Unique
    private static boolean anthology$isInsidePortal(LocalPlayer player) {
        return player != null && PortalRollClientState.isEntityInsidePortal(Minecraft.getInstance(), player);
    }
}
