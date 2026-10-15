package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.gravity.GravityApi;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile")
@Pseudo
public abstract class IronsSpellbooksConeGravityMixin {
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;subtract(DDD)Lnet/minecraft/world/phys/Vec3;"), index = 1)
    private double anthology$invertConeOffset(double offset) {
        Entity owner = ((Projectile) (Object) this).getOwner();
        return owner != null && GravityApi.isGravityInverted(owner) ? -offset : offset;
    }
}
