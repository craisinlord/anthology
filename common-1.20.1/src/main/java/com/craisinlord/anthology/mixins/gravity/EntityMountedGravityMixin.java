package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.MountedGravityHelper;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMountedGravityMixin {
    @Unique
    private Entity anthology$previousGravityVehicle;

    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", at = @At("RETURN"))
    private void anthology$syncMountedGravity(Entity vehicle, boolean force, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            MountedGravityHelper.syncConnectedStack((Entity) (Object) this);
        }
    }

    @Inject(method = "removeVehicle", at = @At("HEAD"))
    private void anthology$captureGravityVehicle(CallbackInfo ci) {
        this.anthology$previousGravityVehicle = ((Entity) (Object) this).getVehicle();
    }

    @Inject(method = "removeVehicle", at = @At("RETURN"))
    private void anthology$syncDetachedGravity(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        Entity vehicle = this.anthology$previousGravityVehicle;
        this.anthology$previousGravityVehicle = null;
        if (vehicle != null && entity.getVehicle() != vehicle) {
            MountedGravityHelper.syncSeparatedStacks(vehicle, entity);
        }
    }
}
