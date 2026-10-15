package com.craisinlord.anthology.mixins.multipart;

import com.craisinlord.anthology.multipart.MultipartEntityOwner;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)

public abstract class EntityMultipartLifecycleMixin {
    @Inject(method = "setLevelCallback", at = @At("TAIL"))
    private void anthology$spawnMultipartParts(EntityInLevelCallback entityInLevelCallback, CallbackInfo ci) {
        if ((Object) this instanceof MultipartEntityOwner owner
                && entityInLevelCallback != EntityInLevelCallback.NULL
                && owner.anthology$getMultipartParts() == null) {
            owner.anthology$spawnMultipartParts();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void anthology$syncMultipartParts(CallbackInfo ci) {
        if ((Object) this instanceof MultipartEntityOwner owner) {
            owner.anthology$syncMultipartParts();
        }
    }

    @Inject(method = "setRemoved", at = @At("TAIL"))
    private void anthology$discardMultipartParts(Entity.RemovalReason removalReason, CallbackInfo ci) {
        if ((Object) this instanceof MultipartEntityOwner owner) {
            owner.anthology$discardMultipartParts();
        }
    }
}
