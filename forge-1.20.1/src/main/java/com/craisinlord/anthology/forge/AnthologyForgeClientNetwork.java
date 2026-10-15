package com.craisinlord.anthology.forge;

import com.craisinlord.anthology.network.AnthologyNetwork;
import com.craisinlord.anthology.network.CustomPacketPayload;
import net.minecraft.client.Minecraft;

final class AnthologyForgeClientNetwork {
    private AnthologyForgeClientNetwork() {
    }

    static void handle(CustomPacketPayload payload) {
        AnthologyNetwork.ClientboundPayload<?> entry = AnthologyNetwork.clientbound(payload.type().id());
        if (entry != null) {
            entry.handle(payload, Minecraft.getInstance().player);
        }
    }
}
