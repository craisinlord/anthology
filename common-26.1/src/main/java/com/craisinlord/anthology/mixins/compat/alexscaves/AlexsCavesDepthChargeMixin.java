package com.craisinlord.anthology.mixins.compat.alexscaves;

import com.craisinlord.anthology.gravity.GravityHooks;
import com.craisinlord.anthology.gravity.GravityApi;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "com.github.alexmodguy.alexscaves.server.entity.item.DepthChargeEntity")
@Pseudo
public abstract class AlexsCavesDepthChargeMixin {
    @ModifyArg(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"),
            index = 1,
            require = 0
    )
    private double anthology$invertManualGravity(double y) {
        Projectile self = (Projectile) (Object) this;
        Entity owner = self.getOwner();
        return !self.isNoGravity() && (GravityHooks.isInInvertedRegion(self)
                || (owner != null && GravityApi.isGravityInverted(owner))) ? -y : y;
    }
}
