package com.craisinlord.anthology.mixins.portal;

import com.craisinlord.anthology.portal.PortalEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Projectile.class)
public abstract class ProjectilePortalHitMixin {
    @Inject(method = "hitTargetOrDeflectSelf", at = @At("HEAD"), cancellable = true)
    private void anthology$passThroughPortal(HitResult hit, CallbackInfoReturnable<ProjectileDeflection> cir) {
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit
                && PortalEntity.passProjectileThroughPortal((Projectile) (Object) this, blockHit)) {
            cir.setReturnValue(ProjectileDeflection.NONE);
        }
    }
}
