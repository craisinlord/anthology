package com.craisinlord.anthology.effect;

import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityDirection;
import com.craisinlord.anthology.gravity.GravityTransition;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;

public final class InvertedMobEffect extends MobEffect {
    private static final GravityTransition EFFECT_TRANSITION = new GravityTransition(12);

    public InvertedMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.getType() == EntityType.VEX) {
            if (!entity.level().isClientSide()) {
                GravityApi.clearForcedGravity(entity);
            }
            return true;
        }
        if (!entity.level().isClientSide()) {
            if (GravityApi.getGravityDirection(entity) != GravityDirection.UP || !GravityApi.isGravityForced(entity)) {
                GravityApi.setForcedGravityDirection(entity, GravityDirection.UP, EFFECT_TRANSITION);
            }
        } else {
            if (!GravityApi.isGravityInverted(entity)) {
                GravityApi.applySyncedState(entity, GravityDirection.UP, GravityDirection.UP, true, 0, 0);
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
