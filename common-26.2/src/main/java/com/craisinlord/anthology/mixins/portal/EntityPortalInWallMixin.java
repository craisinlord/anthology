package com.craisinlord.anthology.mixins.portal;

import com.craisinlord.anthology.portal.PortalEntity;
import com.craisinlord.anthology.portal.PortalRegistry;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityPortalInWallMixin {
    @Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
    private void anthology$ignorePortalInWall(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity.level() == null || !PortalRegistry.hasAny(entity.level())) {
            return;
        }
        for (PortalEntity portal : PortalRegistry.near(entity.level(), entity.getBoundingBox().inflate(2.0D))) {
            if (portal.intersectsEntityBounds(entity) || portal.getCollisionRemovalAabbForEntity(entity).intersects(entity.getBoundingBox())) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
