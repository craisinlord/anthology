package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.craisinlord.anthology.time.TimeDilationManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileTimeDilationMixin {
    @Inject(method = "setOwner(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
    private void anthology$inheritTimeDilation(Entity owner, CallbackInfo callbackInfo) {
        if (owner != null) {
            TimeDilationApi.setInheritedRate((Entity) (Object) this, TimeDilationApi.getRate(owner));
            TimeDilationManager.trackPotentiallyAffected((Entity) (Object) this);
        }
    }
}
