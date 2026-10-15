package com.craisinlord.anthology.mixins.portal;

import com.craisinlord.anthology.portal.PortalEntity;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowPortalHitMixin {
    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;onHit(Lnet/minecraft/world/phys/HitResult;)V"))
    private boolean anthology$passThroughPortal(AbstractArrow projectile, HitResult hit) {
        return !PortalEntity.passProjectileHitThroughPortal(projectile, hit);
    }
}
