package com.craisinlord.anthology.time;

import com.craisinlord.anthology.network.AnthologyNetwork;
import com.craisinlord.anthology.time.client.ClientTimeDilationTicker;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class TimeDilationSync {
    private TimeDilationSync() {
    }

    public static void register() {
        AnthologyNetwork.registerClientbound(TimeDilationRatePayload.TYPE, TimeDilationRatePayload.STREAM_CODEC,
                (payload, player) -> TimeDilationApi.applySyncedRate(payload.entityUuid(), payload.rate()));
        AnthologyNetwork.registerClientbound(TimeDilationFieldsPayload.TYPE, TimeDilationFieldsPayload.STREAM_CODEC,
                (payload, player) -> ClientTimeDilationTicker.applyFields(payload.fields()));
    }

    public static void syncRate(Entity entity, double rate) {
        AnthologyNetwork.sendToTrackingAndSelf(entity, new TimeDilationRatePayload(entity.getUUID(), rate));
    }

    public static void syncRateToPlayer(ServerPlayer player, Entity entity) {
        if (entity instanceof TimeDilationEntityAccess access) {
            AnthologyNetwork.sendToPlayer(player, new TimeDilationRatePayload(entity.getUUID(), access.anthology$getTimeDilationRate()));
        }
    }

    public static void syncFields(ServerPlayer player, List<TimeDilationFieldSnapshot> fields) {
        AnthologyNetwork.sendToPlayer(player, new TimeDilationFieldsPayload(fields));
    }
}
