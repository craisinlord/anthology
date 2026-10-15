package com.craisinlord.anthology.mixins.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Creeper.class)
public abstract class CreeperScaleExplosionMixin {
    @Unique
    private static final float ANTHOLOGY$MIN_EXPLOSION_SCALE = 0.0625F;
    @Unique
    private static final float ANTHOLOGY$MAX_EXPLOSION_SCALE = 4.0F;

    @ModifyArg(
            method = "explodeCreeper",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"),
            index = 4
    )
    private float anthology$scaleExplosionRadius(float radius) {
        return radius * anthology$explosionScale();
    }

    @ModifyArg(
            method = "spawnLingeringCloud",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/AreaEffectCloud;setRadius(F)V")
    )
    private float anthology$scaleCloudRadius(float radius) {
        return radius * anthology$explosionScale();
    }

    @Unique
    private float anthology$explosionScale() {
        return Mth.clamp(((Creeper) (Object) this).getScale(), ANTHOLOGY$MIN_EXPLOSION_SCALE, ANTHOLOGY$MAX_EXPLOSION_SCALE);
    }
}
