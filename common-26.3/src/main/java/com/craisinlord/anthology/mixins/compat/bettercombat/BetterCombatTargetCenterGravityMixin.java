package com.craisinlord.anthology.mixins.compat.bettercombat;

import com.craisinlord.anthology.gravity.GravityApi;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = {
        "net.bettercombat.client.collision.TargetFinder$CollisionFilter",
        "net.bettercombat.client.collision.TargetFinder$RadialFilter"
})
@Pseudo
public abstract class BetterCombatTargetCenterGravityMixin {
    @WrapOperation(require = 0, method = "lambda$filter$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getBbHeight()F"))
    private float anthology$worldHeightOffset(Entity entity, Operation<Float> original) {
        float height = original.call(entity);
        return GravityApi.isGravityInverted(entity) ? -height : height;
    }
}
