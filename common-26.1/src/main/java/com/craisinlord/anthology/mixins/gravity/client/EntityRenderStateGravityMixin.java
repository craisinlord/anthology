package com.craisinlord.anthology.mixins.gravity.client;

import com.craisinlord.anthology.gravity.client.GravityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateGravityMixin implements GravityRenderState {
    @Unique
    private Quaternionf anthology$gravityRotation;

    @Override
    public Quaternionf anthology$getGravityRotation() {
        return this.anthology$gravityRotation;
    }

    @Override
    public void anthology$setGravityRotation(Quaternionf rotation) {
        this.anthology$gravityRotation = rotation;
    }
}
