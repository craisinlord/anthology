package com.craisinlord.anthology.gravity;

import com.craisinlord.anthology.effect.AnthologyEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;

public final class MountedGravityHelper {
    private static final GravityTransition STACK_TRANSITION = new GravityTransition(12);

    private MountedGravityHelper() {
    }

    public static void syncConnectedStack(Entity entity) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        syncRootStack(entity.getRootVehicle(), null);
    }

    public static void syncConnectedStackIgnoring(Entity entity, LivingEntity ignoredEffectSource) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        syncRootStack(entity.getRootVehicle(), ignoredEffectSource);
    }

    public static void syncSeparatedStacks(Entity previousVehicle, Entity detachedEntity) {
        if (previousVehicle != null && !previousVehicle.level().isClientSide()) {
            syncRootStack(previousVehicle.getRootVehicle(), null);
        }
        if (detachedEntity != null && !detachedEntity.level().isClientSide()) {
            syncRootStack(detachedEntity.getRootVehicle(), null);
        }
    }

    private static void syncRootStack(Entity root, LivingEntity ignoredEffectSource) {
        boolean shouldInvert = stackHasInvertedEffect(root, ignoredEffectSource);
        applyGravityState(root, shouldInvert);
        for (Entity passenger : root.getIndirectPassengers()) {
            applyGravityState(passenger, shouldInvert);
        }
    }

    private static boolean stackHasInvertedEffect(Entity root, LivingEntity ignoredEffectSource) {
        if (hasInvertedEffect(root, ignoredEffectSource)) {
            return true;
        }
        for (Entity passenger : root.getIndirectPassengers()) {
            if (hasInvertedEffect(passenger, ignoredEffectSource)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasInvertedEffect(Entity entity, LivingEntity ignoredEffectSource) {
        return entity instanceof LivingEntity living
                && living != ignoredEffectSource
                && living.getType() != EntityTypes.VEX
                && living.hasEffect(AnthologyEffects.inverted());
    }

    private static void applyGravityState(Entity entity, boolean shouldInvert) {
        GravityAccess access = (GravityAccess) entity;
        if (shouldInvert && entity.getType() != EntityTypes.VEX) {
            if (!access.anthology$isEffectGravityManaged()) {
                if (GravityApi.isGravityForced(entity)) {
                    return;
                }
                access.anthology$setEffectGravityManaged(true, GravityApi.getGravityDirection(entity));
            }
            if (GravityApi.getGravityDirection(entity) != GravityDirection.UP || !GravityApi.isGravityForced(entity)) {
                access.anthology$setGravityState(GravityDirection.UP, true, STACK_TRANSITION);
            }
            return;
        }
        if (access.anthology$isEffectGravityManaged()) {
            GravityDirection previousDirection = access.anthology$getGravityBeforeEffect();
            access.anthology$setEffectGravityManaged(false, GravityDirection.DOWN);
            access.anthology$setGravityState(previousDirection, false, STACK_TRANSITION);
        }
    }
}
