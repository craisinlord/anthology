package com.craisinlord.anthology.mixins.portal.client;

import net.minecraft.client.Camera;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraPortalViewAccessor {
    @Accessor("initialized")
    void anthology$setInitialized(boolean initialized);

    @Accessor("detached")
    void anthology$setDetached(boolean detached);

    @Invoker("createProjectionMatrixForCulling")
    Matrix4f anthology$createProjectionMatrixForCulling();
}
