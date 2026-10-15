package com.craisinlord.anthology.shake;

import com.craisinlord.anthology.network.AnthologyNetwork;
import com.craisinlord.anthology.shake.client.CameraShake;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class CameraShakeSync {
    private CameraShakeSync() {
    }

    public static void register() {
        AnthologyNetwork.registerClientbound(ImpactShakePayload.TYPE, ImpactShakePayload.STREAM_CODEC,
                (payload, player) -> CameraShake.triggerImpact(
                        new Vec3(payload.x(), payload.y(), payload.z()),
                        payload.intensity(),
                        payload.durationTicks(),
                        payload.radius()
                ));
    }

    public static void send(ServerPlayer player, Vec3 pos, float intensity, int durationTicks, float radius) {
        AnthologyNetwork.sendToPlayer(player, new ImpactShakePayload(pos.x, pos.y, pos.z, intensity, durationTicks, radius));
    }

    public static void sendNearby(ServerLevel level, Vec3 pos, float intensity, int durationTicks, float radius) {
        ImpactShakePayload payload = new ImpactShakePayload(pos.x, pos.y, pos.z, intensity, durationTicks, radius);
        double radiusSqr = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos.x, pos.y, pos.z) <= radiusSqr) {
                AnthologyNetwork.sendToPlayer(player, payload);
            }
        }
    }
}
