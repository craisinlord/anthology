package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.gravity.GravityHooks;
import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityDirection;
import com.craisinlord.anthology.gravity.GravityRotationUtil;
import com.craisinlord.anthology.gravity.GravityTransition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile")
@Pseudo
public abstract class IronsSpellbooksProjectileGravityMixin {
    @Shadow public abstract float getSpeed();
    @Shadow protected abstract double getDefaultGravity();

    @Inject(method = "tick", at = @At("HEAD"))
    private void anthology$prepareMagicProjectile(CallbackInfo ci) {
        Projectile self = (Projectile) (Object) this;
        Entity owner = self.getOwner();
        boolean inverted = GravityHooks.isInInvertedRegion(self)
                || (owner != null && GravityApi.isGravityInverted(owner));
        if (!inverted) {
            return;
        }
        GravityApi.setAirborneGravityDirection(
                self,
                GravityDirection.UP,
                false,
                GravityTransition.INSTANT
        );
    }

    @Inject(method = "shoot", at = @At("HEAD"), cancellable = true)
    private void anthology$rotateMagicProjectileLaunch(Vec3 rotation, CallbackInfo ci) {
        Projectile self = (Projectile) (Object) this;
        Entity owner = self.getOwner();
        if (owner == null || (!GravityHooks.isInInvertedRegion(self)
                && !GravityApi.isGravityInverted(owner))) {
            return;
        }
        self.setDeltaMovement(GravityRotationUtil.vecPlayerToWorld(
                rotation,
                GravityApi.getGravityDirection(owner)
        ).scale(this.getSpeed()));
        ci.cancel();
    }

    @Inject(method = "travel", at = @At("TAIL"))
    private void anthology$invertMagicProjectileGravity(CallbackInfo ci) {
        Projectile self = (Projectile) (Object) this;
        if (!GravityApi.isGravityInverted(self) || self.isNoGravity()) {
            return;
        }

        self.setDeltaMovement(self.getDeltaMovement().add(0.0D, 2.0D * this.getDefaultGravity(), 0.0D));
    }
}
