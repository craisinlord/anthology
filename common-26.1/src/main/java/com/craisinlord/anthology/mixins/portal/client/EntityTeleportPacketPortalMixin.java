package com.craisinlord.anthology.mixins.portal.client;

import com.craisinlord.anthology.portal.PortalEntity;
import com.craisinlord.anthology.portal.PortalShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class EntityTeleportPacketPortalMixin {
    @Inject(method = "handleTeleportEntity", at = @At("TAIL"))
    private void anthology$snapPortalTransitEntity(ClientboundTeleportEntityPacket packet, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(packet.id());
        if (entity == null || packet.relatives().contains(Relative.X) || packet.relatives().contains(Relative.Y) || packet.relatives().contains(Relative.Z)) {
            return;
        }
        Vec3 destination = packet.change().position();
        anthology$snapToPortalExit(entity, destination);
    }

    private static void anthology$snapToPortalExit(Entity entity, Vec3 destination) {
        if (!isNearPortalExit(entity, destination)) {
            return;
        }
        InterpolationHandler interpolation = entity.getInterpolation();
        if (interpolation != null) {
            interpolation.cancel();
        }
        entity.setPortalCooldown(PortalEntity.TELEPORT_COOLDOWN_TICKS);
        entity.setPos(destination.x, destination.y, destination.z);
        entity.xo = destination.x;
        entity.yo = destination.y;
        entity.zo = destination.z;
        entity.yRotO = entity.getYRot();
        entity.xRotO = entity.getXRot();
        if (entity instanceof LivingEntity livingEntity) {
            LivingEntityPortalLerpAccessor accessor = (LivingEntityPortalLerpAccessor) livingEntity;
            accessor.anthology$setLerpHeadSteps(0);
        }
    }

    private static boolean isNearPortalExit(Entity entity, Vec3 destination) {
        Vec3 offset = destination.subtract(entity.position());
        double entityExtent = Math.max(entity.getBbWidth(), entity.getBbHeight());
        double edgeAllowance = entityExtent * 0.5D;
        AABB search = entity.getBoundingBox().move(offset).inflate(entityExtent + PortalShape.DEFAULT_SCAN_DISTANCE);
        Vec3 probe = destination.add(0.0D, entity.getEyeHeight(), 0.0D);
        for (PortalEntity portal : com.craisinlord.anthology.portal.PortalRegistry.near(entity.level(), search)) {
            PortalShape.PortalLocalCoords coords = portal.getWorldPortalShape().localCoords(probe);
            if (coords.depth() >= -entityExtent - 0.5D
                    && coords.depth() <= PortalShape.DEFAULT_SCAN_DISTANCE + 0.5D
                    && Math.abs(coords.horizontal()) <= portal.getWorldPortalShape().halfWidth() + edgeAllowance
                    && Math.abs(coords.vertical()) <= portal.getWorldPortalShape().halfHeight() + edgeAllowance) {
                return true;
            }
        }
        return false;
    }
}
