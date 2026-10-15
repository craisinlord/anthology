package com.craisinlord.anthology.gravity.client;

import org.joml.Quaternionf;

public interface GravityRenderState {
    Quaternionf anthology$getGravityRotation();

    void anthology$setGravityRotation(Quaternionf rotation);
}
