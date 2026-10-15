package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.MountedGravityHelper;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMountedGravityMixin {
    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", at = @At("RETURN"))
    private void anthology$syncMountedGravity(Entity vehicle, boolean force, boolean sendEvent, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            MountedGravityHelper.syncConnectedStack((Entity) (Object) this);
        }
    }

    @WrapMethod(method = "removeVehicle")
    private void anthology$syncDetachedGravity(Operation<Void> original) {
        Entity entity = (Entity) (Object) this;
        Entity vehicle = entity.getVehicle();
        original.call();
        if (vehicle != null) {
            MountedGravityHelper.syncSeparatedStacks(vehicle, entity);
        }
    }
}
