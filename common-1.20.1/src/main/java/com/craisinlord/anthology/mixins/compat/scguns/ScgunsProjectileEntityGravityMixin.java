package com.craisinlord.anthology.mixins.compat.scguns;

import com.craisinlord.anthology.gravity.GravityHooks;
import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityDirection;
import com.craisinlord.anthology.gravity.GravityRotationUtil;
import com.craisinlord.anthology.gravity.GravityTransition;
import com.craisinlord.anthology.gravity.ScgunsProjectileAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
        "top.ribs.scguns.entity.projectile.ProjectileEntity",
        "top.ribs.scguns.entity.projectile.BearPackShellProjectileEntity",
        "top.ribs.scguns.entity.projectile.KrahgRoundProjectileEntity",
        "top.ribs.scguns.entity.projectile.LightningProjectileEntity",
        "top.ribs.scguns.entity.projectile.OsborneSlugProjectileEntity",
        "top.ribs.scguns.entity.projectile.ShotballProjectileEntity"
})
@Pseudo
public abstract class ScgunsProjectileEntityGravityMixin {
    @Unique private boolean anthology$launchDirectionAdjusted;

    @Inject(method = {"tick", "m_8119_"}, at = @At("HEAD"))
    private void anthology$prepareScgunsProjectile(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        LivingEntity shooter = ((ScgunsProjectileAccess) self).anthology$getScgunsShooter();
        boolean inverted = GravityHooks.isInInvertedRegion(self)
                || (shooter != null && GravityApi.isGravityInverted(shooter));
        if (!inverted) {
            if (GravityApi.isGravityInverted(self) && !GravityApi.isGravityForced(self)) {
                GravityApi.setAirborneGravityDirection(
                        self,
                        GravityDirection.DOWN,
                        false,
                        GravityTransition.INSTANT
                );
            }
            return;
        }

        GravityApi.setAirborneGravityDirection(
                self,
                GravityDirection.UP,
                false,
                GravityTransition.INSTANT
        );
        if (!this.anthology$launchDirectionAdjusted && self.tickCount <= 1) {
            Vec3 motion = self.getDeltaMovement();
            if (motion.lengthSqr() > 1.0E-8D) {
                self.setDeltaMovement(GravityRotationUtil.vecPlayerToWorld(
                        motion,
                        GravityDirection.UP
                ));
            }
            this.anthology$launchDirectionAdjusted = true;
        }
    }

    @Inject(method = {"tick", "m_8119_"}, at = @At("TAIL"))
    private void anthology$invertScgunsGravity(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        double modifiedGravity = ((ScgunsProjectileAccess) self).anthology$getScgunsModifiedGravity();
        if (!GravityApi.isGravityInverted(self) || modifiedGravity == 0.0D) {
            return;
        }

        self.setDeltaMovement(self.getDeltaMovement().add(0.0D, -2.0D * modifiedGravity, 0.0D));
    }
}
