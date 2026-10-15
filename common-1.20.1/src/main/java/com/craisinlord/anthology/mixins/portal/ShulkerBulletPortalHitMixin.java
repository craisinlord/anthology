package com.craisinlord.anthology.mixins.portal;

import com.craisinlord.anthology.portal.PortalEntity;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShulkerBullet.class)
public abstract class ShulkerBulletPortalHitMixin {
    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ShulkerBullet;onHit(Lnet/minecraft/world/phys/HitResult;)V"))
    private boolean anthology$passThroughPortal(ShulkerBullet projectile, HitResult hit) {
        return !PortalEntity.passProjectileHitThroughPortal(projectile, hit);
    }
}
