package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.craisinlord.anthology.time.TimeDilationMath;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractHurtingProjectile.class)
public abstract class HurtingProjectileMovementTimeDilationMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/hurtingprojectile/AbstractHurtingProjectile;setPos(Lnet/minecraft/world/phys/Vec3;)V"))
    private void anthology$slowPosition(AbstractHurtingProjectile projectile, Vec3 position) {
        double rate = Math.max(TimeDilationMath.MIN_RATE, TimeDilationApi.getRate(projectile));
        if (Math.abs(rate - TimeDilationMath.NORMAL_RATE) < 0.001D) {
            projectile.setPos(position);
            return;
        }
        projectile.setPos(projectile.position().lerp(position, rate));
    }
}
