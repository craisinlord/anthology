package com.craisinlord.anthology.mixins.time;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticlePositionAccessor {
    @Accessor("x")
    double anthology$getX();

    @Accessor("y")
    double anthology$getY();

    @Accessor("z")
    double anthology$getZ();
}
