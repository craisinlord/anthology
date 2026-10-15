package com.craisinlord.anthology.mixins.portal;

import com.craisinlord.anthology.portal.PortalEntity;
import com.craisinlord.anthology.portal.PortalRegistry;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PlayerMovePacketPortalMixin {
    @Unique
    private static final double MAX_CLAIMED_MOVEMENT_SQR = 100.0D;
    @Unique
    private AABB anthology$portalMoveStartBox;
    @Unique
    private Vec3 anthology$portalMoveStartPosition;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void anthology$capturePortalMoveStart(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        ServerPlayer player = ((ServerGamePacketListenerImpl) (Object) this).player;
        if (player.level().getServer() == null || !player.level().getServer().isSameThread()) {
            return;
        }
        this.anthology$portalMoveStartBox = null;
        this.anthology$portalMoveStartPosition = null;
        if (!packet.hasPosition() || !PortalRegistry.hasAny(player.level()) || ((ServerGamePacketListenerTeleportAccessor) this).anthology$getAwaitingPositionFromClient() != null) {
            return;
        }
        this.anthology$portalMoveStartBox = player.getBoundingBox();
        this.anthology$portalMoveStartPosition = player.position();
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void anthology$teleportPlayerOnPortalCrossing(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        AABB startBox = this.anthology$portalMoveStartBox;
        Vec3 startPosition = this.anthology$portalMoveStartPosition;
        this.anthology$portalMoveStartBox = null;
        this.anthology$portalMoveStartPosition = null;
        if (startBox == null || startPosition == null) {
            return;
        }
        ServerPlayer player = ((ServerGamePacketListenerImpl) (Object) this).player;
        Vec3 claimedMovement = new Vec3(packet.getX(startPosition.x), packet.getY(startPosition.y), packet.getZ(startPosition.z))
                .subtract(startPosition);
        double lengthSqr = claimedMovement.lengthSqr();
        if (lengthSqr > 1.0E-10D && lengthSqr < MAX_CLAIMED_MOVEMENT_SQR) {
            PortalEntity.teleportPlayerAfterMovementPacket(player, startBox, claimedMovement);
        }
    }
}
