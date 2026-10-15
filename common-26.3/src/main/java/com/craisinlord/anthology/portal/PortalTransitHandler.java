package com.craisinlord.anthology.portal;

import com.craisinlord.anthology.portal.PortalTransitPayload;
import net.minecraft.server.level.ServerPlayer;

public final class PortalTransitHandler {
    private PortalTransitHandler() {
    }

    public static void handle(ServerPlayer player, PortalTransitPayload payload) {
        if (player == null) {
            return;
        }
        if (!PortalEntity.handleClientTransit(player, payload.portalId(), payload.start(), payload.end(), payload.velocity())) {
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        }
    }
}
