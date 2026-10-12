package com.craisinlord.anthology.portal;

import com.craisinlord.anthology.network.AnthologyNetwork;

public final class PortalEngine {
    private PortalEngine() {
    }

    public static void register() {
        AnthologyNetwork.registerServerbound(PortalTransitPayload.TYPE, PortalTransitPayload.STREAM_CODEC,
                (payload, player) -> PortalTransitHandler.handle(player, payload));
    }
}
