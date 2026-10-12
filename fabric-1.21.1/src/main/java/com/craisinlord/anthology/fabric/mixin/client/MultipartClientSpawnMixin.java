package com.craisinlord.anthology.fabric.mixin.client;

import com.craisinlord.anthology.fabric.AnthologyFabricEntities;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class MultipartClientSpawnMixin {
    @Inject(method = "handleAddEntity", at = @At("HEAD"), cancellable = true)
    private void anthology$skipTrackedMultipartPlaceholders(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        if (packet.getType() == AnthologyFabricEntities.MULTIPART_PART) {
            ci.cancel();
        }
    }
}
