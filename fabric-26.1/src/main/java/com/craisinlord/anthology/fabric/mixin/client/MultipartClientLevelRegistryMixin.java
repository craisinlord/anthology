package com.craisinlord.anthology.fabric.mixin.client;

import com.craisinlord.anthology.multipart.MultipartEntityOwner;
import com.craisinlord.anthology.fabric.client.MultipartClientPartLevelRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MultipartClientLevelRegistryMixin {
    @Inject(method = "setLevelCallback", at = @At("TAIL"))
    private void anthology$registerMultipartParts(EntityInLevelCallback entityInLevelCallback, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof MultipartEntityOwner owner
                && entityInLevelCallback != EntityInLevelCallback.NULL
                && self.level().isClientSide()) {
            if (owner.anthology$getMultipartParts() == null) {
                owner.anthology$spawnMultipartParts();
            }
            MultipartClientPartLevelRegistry.register(owner);
        }
    }

    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void anthology$unregisterMultipartParts(Entity.RemovalReason removalReason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof MultipartEntityOwner owner && self.level().isClientSide()) {
            MultipartClientPartLevelRegistry.unregister(owner);
        }
    }
}
