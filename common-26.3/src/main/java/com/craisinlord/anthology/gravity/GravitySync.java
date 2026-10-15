package com.craisinlord.anthology.gravity;

import com.craisinlord.anthology.network.AnthologyNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class GravitySync {
    private GravitySync() {
    }

    public static void register() {
        AnthologyNetwork.registerClientbound(GravityStatePayload.TYPE, GravityStatePayload.STREAM_CODEC, GravitySync::handle);
    }

    public static void syncEntity(Entity entity) {
        AnthologyNetwork.sendToTrackingAndSelf(entity, payloadFor(entity));
    }

    public static void syncToPlayer(ServerPlayer target, Entity entity) {
        AnthologyNetwork.sendToPlayer(target, payloadFor(entity));
    }

    private static GravityStatePayload payloadFor(Entity entity) {
        return new GravityStatePayload(
                entity.getId(),
                GravityApi.getGravityDirection(entity),
                GravityApi.getPrevGravityDirection(entity),
                GravityApi.isGravityForced(entity),
                GravityApi.getTransitionDuration(entity),
                GravityApi.getTransitionRemaining(entity)
        );
    }

    private static void handle(GravityStatePayload payload, Player player) {
        Entity entity = player.level().getEntity(payload.entityId());
        if (entity == null && player.getId() == payload.entityId()) {
            entity = player;
        }
        if (entity == null) {
            return;
        }
        GravityApi.applySyncedState(
                entity,
                payload.direction(),
                payload.previousDirection(),
                payload.forced(),
                payload.transitionDuration(),
                payload.transitionRemaining()
        );
    }
}
