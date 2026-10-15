package com.craisinlord.anthology.mixins.compat.alexscaves;

import com.craisinlord.anthology.gravity.GravityHooks;
import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityDirection;
import com.craisinlord.anthology.gravity.GravityRotationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.github.alexmodguy.alexscaves.server.entity.item.CandyCaneHookEntity")
@Pseudo
public abstract class AlexsCavesCandyCaneHookMixin {
    private boolean anthology$launchAdjusted;

    @Inject(method = {"tick", "m_8119_"}, at = @At("HEAD"))
    private void anthology$rotateManualLaunch(CallbackInfo ci) {
        Projectile self = (Projectile) (Object) this;
        if (this.anthology$launchAdjusted) {
            return;
        }
        this.anthology$launchAdjusted = true;

        Entity owner = self.getOwner();
        boolean ownerInverted = owner != null && GravityApi.isGravityInverted(owner);
        if (ownerInverted || GravityHooks.isInInvertedRegion(self)) {
            GravityDirection direction = ownerInverted
                    ? GravityApi.getGravityDirection(owner)
                    : GravityDirection.UP;
            Vec3 launch = GravityRotationUtil.vecPlayerToWorld(
                    self.getDeltaMovement(), direction);
            self.setDeltaMovement(launch);
        }
    }

    @ModifyArg(
            method = "fling",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"),
            index = 1
    )
    private double anthology$invertFlingBoost(double y) {
        Projectile self = (Projectile) (Object) this;
        Entity owner = self.getOwner();
        return y != 0.0D && (GravityHooks.isInInvertedRegion(self)
                || (owner != null && GravityApi.isGravityInverted(owner))) ? -y : y;
    }
}
