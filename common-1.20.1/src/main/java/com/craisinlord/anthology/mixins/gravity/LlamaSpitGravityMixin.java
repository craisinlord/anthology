package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityHooks;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.LlamaSpit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LlamaSpit.class)
public abstract class LlamaSpitGravityMixin {
    @ModifyArg(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;",
            ordinal = 0), index = 1)
    private double anthology$applyGravityUpward(double y) {
        return GravityHooks.shouldInvertProjectileGravity((Projectile) (Object) this) ? -y : y;
    }
}
