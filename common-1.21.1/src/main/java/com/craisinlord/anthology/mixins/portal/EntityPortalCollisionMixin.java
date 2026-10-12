package com.craisinlord.anthology.mixins.portal;

import com.craisinlord.anthology.portal.PortalCollision;
import com.craisinlord.anthology.portal.PortalEntity;
import com.craisinlord.anthology.portal.PortalRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Entity.class, priority = 1100)
public abstract class EntityPortalCollisionMixin {
    @Unique
    private AABB anthology$portalGunStartBox;
    @Unique
    private AABB anthology$portalGunMoveStartBox;
    @Unique
    private Vec3 anthology$portalGunMoveStartVelocity;

    @Inject(method = "move", at = @At("HEAD"))
    private void anthology$capturePortalGunMovementStart(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (!PortalRegistry.hasAny(entity.level())) {
            this.anthology$portalGunMoveStartBox = null;
            this.anthology$portalGunMoveStartVelocity = null;
            return;
        }
        this.anthology$portalGunMoveStartBox = entity.getBoundingBox();
        this.anthology$portalGunMoveStartVelocity = entity.getDeltaMovement();
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void anthology$teleportMobOnPortalCrossing(MoverType type, Vec3 movement, CallbackInfo ci) {
        if (this.anthology$portalGunMoveStartBox == null) {
            return;
        }
        AABB startBox = this.anthology$portalGunMoveStartBox;
        Vec3 startVelocity = this.anthology$portalGunMoveStartVelocity;
        this.anthology$portalGunMoveStartBox = null;
        this.anthology$portalGunMoveStartVelocity = null;
        if (startBox != null && startVelocity != null) {
            PortalEntity.teleportEntityAfterMovement((Entity) (Object) this, startBox, startVelocity);
        }
    }

    @Inject(method = "collide", at = @At("HEAD"))
    private void anthology$capturePortalGunStartBox(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        Entity entity = (Entity) (Object) this;
        this.anthology$portalGunStartBox = PortalRegistry.hasAny(entity.level()) ? entity.getBoundingBox() : null;
    }

    @Inject(method = "collide", at = @At("RETURN"), cancellable = true)
    private void anthology$restorePortalCrossingMotion(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        if (this.anthology$portalGunStartBox == null) {
            return;
        }
        Entity entity = (Entity) (Object) this;
        Vec3 collided = cir.getReturnValue();
        Vec3 adjusted = PortalCollision.resolveCollision(entity, this.anthology$portalGunStartBox, movement, collided);
        if (adjusted != collided) {
            cir.setReturnValue(adjusted);
        }
    }
}
