package com.craisinlord.anthology.portal.client;

import com.craisinlord.anthology.portal.PortalTransitPayload;
import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.portal.PortalEntity;
import com.craisinlord.anthology.portal.PortalTransforms;
import com.craisinlord.anthology.portal.PortalTransitMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PortalClientTransit {
    private static long cooldownUntil;

    private PortalClientTransit() {
    }

    public static void afterLocalMove(LocalPlayer player, AABB startBox, Vec3 velocity) {
        if (player.level() == null || !player.isAlive() || player.isPassenger() || player.isSpectator()) {
            return;
        }
        long gameTime = player.level().getGameTime();
        if (gameTime < cooldownUntil && cooldownUntil - gameTime <= PortalEntity.TELEPORT_COOLDOWN_TICKS) {
            return;
        }
        AABB currentBox = player.getBoundingBox();
        if (velocity.lengthSqr() <= 1.0E-12D && startBox.getCenter().distanceToSqr(currentBox.getCenter()) <= 1.0E-10D) {
            return;
        }
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return;
        }
        Vec3 crossingVelocity = GravityApi.isGravityInverted(player)
                ? PortalTransitMath.entityVelocityToWorld(player, velocity)
                : velocity;
        for (PortalEntity portal : PortalEntity.findPortalsNearBounds(player.level(), startBox.minmax(currentBox).expandTowards(crossingVelocity).inflate(1.0D))) {
            PortalEntity linked = portal.getLinkedPortal();
            if (linked == null || !linked.isAlive()) {
                continue;
            }
            if (portal.resolveCrossing(player, startBox, currentBox, crossingVelocity) == null) {
                continue;
            }
            Vec3 look = player.getLookAngle();
            connection.send(new ServerboundCustomPayloadPacket(new PortalTransitPayload(
                    portal.getId(), PortalTransitMath.entityPosition(startBox, player), player.position(), velocity)));
            PortalEntity.PortalTransit transit = portal.computeTransit(player, linked, currentBox, velocity, look);
            Vec3 rollLook = GravityApi.isGravityInverted(player)
                    ? PortalTransitMath.entityVelocityToWorld(player, look)
                    : look;
            PortalRollClientState.onTransit(portal, linked, rollLook);
            apply(player, transit);
            cooldownUntil = gameTime + PortalEntity.TELEPORT_COOLDOWN_TICKS;
            return;
        }
    }

    private static void apply(LocalPlayer player, PortalEntity.PortalTransit transit) {
        Vec3 position = transit.position();
        float yaw = PortalTransforms.yawFromLook(transit.look());
        float pitch = PortalTransforms.pitchFromLook(transit.look());
        player.setPos(position.x, position.y, position.z);
        player.xo = position.x;
        player.yo = position.y;
        player.zo = position.z;
        player.xOld = position.x;
        player.yOld = position.y;
        player.zOld = position.z;
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.setYHeadRot(yaw);
        player.yHeadRotO = yaw;
        player.setYBodyRot(yaw);
        player.yBodyRotO = yaw;
        player.setDeltaMovement(transit.velocity());
        Vec3 fallVelocity = PortalTransitMath.entityVelocityToWorld(player, transit.velocity());
        player.fallDistance = PortalTransitMath.exitFallDistance((float) player.fallDistance, fallVelocity, player);
        player.setOnGround(false);
        player.horizontalCollision = false;
        player.verticalCollision = false;
        player.verticalCollisionBelow = false;
        player.setPortalCooldown(PortalEntity.TELEPORT_COOLDOWN_TICKS);
    }
}
