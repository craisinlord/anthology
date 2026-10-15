package com.craisinlord.anthology.fabric;

import com.craisinlord.anthology.network.CustomPacketPayload;
import java.util.function.Consumer;

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
