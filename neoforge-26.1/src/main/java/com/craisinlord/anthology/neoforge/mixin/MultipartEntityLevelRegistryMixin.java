package com.craisinlord.anthology.neoforge.mixin;

import com.craisinlord.anthology.multipart.MultipartEntityOwner;
import com.craisinlord.anthology.neoforge.multipart.MultipartPartLevelRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)

public abstract class MultipartEntityLevelRegistryMixin {
    @Inject(method = "setLevelCallback", at = @At("TAIL"))
    private void anthology$registerMultipartParts(EntityInLevelCallback entityInLevelCallback, CallbackInfo ci) {
        if ((Object) this instanceof MultipartEntityOwner owner
                && entityInLevelCallback != EntityInLevelCallback.NULL) {
            if (owner.anthology$getMultipartParts() == null) {
                owner.anthology$spawnMultipartParts();
            }
            MultipartPartLevelRegistry.register(owner);
        }
    }

    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void anthology$unregisterMultipartParts(Entity.RemovalReason removalReason, CallbackInfo ci) {
        if ((Object) this instanceof MultipartEntityOwner owner) {
            MultipartPartLevelRegistry.unregister(owner);
        }
    }
}
