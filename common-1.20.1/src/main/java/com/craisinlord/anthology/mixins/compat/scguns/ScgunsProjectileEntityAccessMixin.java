package com.craisinlord.anthology.mixins.compat.scguns;

import com.craisinlord.anthology.gravity.ScgunsProjectileAccess;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "top.ribs.scguns.entity.projectile.ProjectileEntity")
@Pseudo
public abstract class ScgunsProjectileEntityAccessMixin implements ScgunsProjectileAccess {
    @Shadow(remap = false) @Nullable protected LivingEntity shooter;
    @Shadow(remap = false) protected double modifiedGravity;

    @Override
    public @Nullable LivingEntity anthology$getScgunsShooter() {
        return this.shooter;
    }

    @Override
    public double anthology$getScgunsModifiedGravity() {
        return this.modifiedGravity;
    }
}
