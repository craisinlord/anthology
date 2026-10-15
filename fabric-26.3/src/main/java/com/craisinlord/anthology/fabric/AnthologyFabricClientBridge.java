package com.craisinlord.anthology.fabric;

import java.util.function.Consumer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class AnthologyFabricClientBridge {
    private static Consumer<CustomPacketPayload> serverSender = payload -> {
    };

    private AnthologyFabricClientBridge() {
    }

    public static void setServerSender(Consumer<CustomPacketPayload> sender) {
        serverSender = sender;
    }

    static void sendToServer(CustomPacketPayload payload) {
        serverSender.accept(payload);
    }
}
