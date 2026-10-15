package com.craisinlord.anthology.mixins.compat.ars_nouveau;

import com.craisinlord.anthology.gravity.GravityHooks;
import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityDirection;
import com.craisinlord.anthology.gravity.GravityTransition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell")
@Pseudo
public abstract class ArsNouveauProjectileGravityMixin {
    @Inject(method = {"tick", "m_8119_"}, at = @At("HEAD"))
    private void anthology$prepareArsProjectile(CallbackInfo ci) {
        Projectile self = (Projectile) (Object) this;
        Entity owner = self.getOwner();
        if (owner != null && (GravityHooks.isInInvertedRegion(self)
                || GravityApi.isGravityInverted(owner))) {
            GravityApi.setAirborneGravityDirection(
                    self,
                    GravityDirection.UP,
                    false,
                    GravityTransition.INSTANT
            );
        }
    }

    @Inject(method = "tickNextPosition", at = @At("TAIL"))
    private void anthology$invertArsProjectileGravity(CallbackInfo ci) {
        Projectile self = (Projectile) (Object) this;
        if (!GravityApi.isGravityInverted(self) || self.isNoGravity()) {
            return;
        }

        self.setDeltaMovement(self.getDeltaMovement().add(0.0D, 0.06D, 0.0D));
    }
}
