package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityDirection;
import com.craisinlord.anthology.gravity.GravityRotationUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
    @Shadow
    public ServerPlayer player;

    @Shadow
    private double lastGoodX;
    @Shadow
    private double lastGoodY;
    @Shadow
    private double lastGoodZ;

    @ModifyVariable(method = "handlePlayerPositionChange(DDDFFZZ)V",
            at = @At(value = "STORE", ordinal = 0),
            name = "movedUpwards")
    private boolean anthology$fixHandleMovePlayerFlag(boolean originalFlag, double requestedX, double requestedY, double requestedZ, float requestedYRot, float requestedXRot, boolean isOnGround, boolean horizontalCollision) {
        GravityDirection gravity = GravityApi.getGravityDirection(this.player);
        if (!gravity.isInverted()) {
            return originalFlag;
        }
        double dx = requestedX - this.lastGoodX;
        double dy = requestedY - this.lastGoodY;
        double dz = requestedZ - this.lastGoodZ;
        Vec3 localVec = GravityRotationUtil.vecWorldToPlayer(new Vec3(dx, dy, dz), gravity);
        return localVec.y > 0.0;
    }

    @ModifyArg(method = "handlePlayerPositionChange(DDDFFZZ)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
    private Vec3 anthology$fixHandleMovePlayerVector(Vec3 worldDelta) {
        GravityDirection gravity = GravityApi.getGravityDirection(this.player);
        if (!gravity.isInverted()) {
            return worldDelta;
        }
        return GravityRotationUtil.vecWorldToPlayer(worldDelta, gravity);
    }

    @ModifyArg(method = "handlePlayerPositionChange(DDDFFZZ)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;setOnGroundWithMovement(ZZLnet/minecraft/world/phys/Vec3;)V"),
            index = 0)
    private boolean anthology$fixHandleMovePlayerOnGround(boolean originalOnGround) {
        GravityDirection gravity = GravityApi.getGravityDirection(this.player);
        if (!gravity.isInverted()) {
            return originalOnGround;
        }
        return originalOnGround || this.player.verticalCollisionBelow || this.player.verticalCollision;
    }

    @ModifyArg(method = "handlePlayerPositionChange(DDDFFZZ)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;doCheckFallDamage(DDDZ)V"),
            index = 3)
    private boolean anthology$fixHandleMovePlayerFallDamageOnGround(boolean originalOnGround) {
        GravityDirection gravity = GravityApi.getGravityDirection(this.player);
        if (!gravity.isInverted()) {
            return originalOnGround;
        }
        return originalOnGround || this.player.verticalCollisionBelow || this.player.verticalCollision;
    }
}
