package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityDirection;
import com.craisinlord.anthology.gravity.GravityRotationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerSneakMixin {
    @Shadow @Final private Abilities abilities;
    @Shadow protected abstract boolean isStayingOnGroundSurface();

    @Inject(method = "maybeBackOffFromEdge", at = @At("HEAD"), cancellable = true)
    private void anthology$fixSneakEdge(Vec3 movement, MoverType type, CallbackInfoReturnable<Vec3> cir) {
        Entity this_ = (Entity) (Object) this;
        GravityDirection direction = GravityApi.getGravityDirection(this_);
        if (!direction.isInverted()) {
            return;
        }
        float step = this_.maxUpStep();
        boolean isNearCeiling;
        if (this_.onGround()) {
            isNearCeiling = true;
        } else {
            float remaining = step - this_.fallDistance;
            if (remaining > 0.0F) {
                AABB bb = this_.getBoundingBox();
                isNearCeiling = !this_.level().noCollision(this_,
                        new AABB(bb.minX, bb.maxY, bb.minZ,
                                 bb.maxX, bb.maxY + remaining + 1.0E-5, bb.maxZ));
            } else {
                isNearCeiling = false;
            }
        }

        if (!this.abilities.flying
                && (type == MoverType.SELF || type == MoverType.PLAYER)
                && this.isStayingOnGroundSurface()
                && isNearCeiling) {
            Vec3 playerMovement = GravityRotationUtil.vecWorldToPlayer(movement, direction);
            double d = playerMovement.x;
            double e = playerMovement.z;

            while (d != 0.0D && canFallAtLeastInverted(this_, d, 0.0D, step, direction)) {
                if (Math.abs(d) <= 0.05D) d = 0.0D;
                else if (d > 0.0D) d -= 0.05D;
                else d += 0.05D;
            }
            while (e != 0.0D && canFallAtLeastInverted(this_, 0.0D, e, step, direction)) {
                if (Math.abs(e) <= 0.05D) e = 0.0D;
                else if (e > 0.0D) e -= 0.05D;
                else e += 0.05D;
            }
            while (d != 0.0D && e != 0.0D && canFallAtLeastInverted(this_, d, e, step, direction)) {
                if (Math.abs(d) <= 0.05D) d = 0.0D;
                else if (d > 0.0D) d -= 0.05D;
                else d += 0.05D;

                if (Math.abs(e) <= 0.05D) e = 0.0D;
                else if (e > 0.0D) e -= 0.05D;
                else e += 0.05D;
            }

            cir.setReturnValue(GravityRotationUtil.vecPlayerToWorld(
                    new Vec3(d, playerMovement.y, e), direction));
        } else {
            cir.setReturnValue(movement);
        }
    }

    private static boolean canFallAtLeastInverted(Entity entity,
                                                   double dx, double dz,
                                                   float step,
                                                   GravityDirection direction) {
        Vec3 worldOffset = GravityRotationUtil.vecPlayerToWorld(
                new Vec3(dx, -step, dz), direction);

        AABB bb = entity.getBoundingBox();
        AABB probe = bb.move(worldOffset);

        return entity.level().noCollision(entity, probe);
    }
}
