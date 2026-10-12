package com.craisinlord.anthology.beam;

import net.minecraft.world.entity.LivingEntity;

public interface BeamOwner {
    default float scaleBeamDamage(float damage) {
        return damage;
    }

    default boolean canBeamDamage(LivingEntity target) {
        return true;
    }
}
