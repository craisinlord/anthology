package com.craisinlord.anthology.gravity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

public final class GravityApi {
    private GravityApi() {
    }

    public static GravityDirection getGravityDirection(Entity entity) {
        return access(entity).anthology$getGravityDirection();
    }

    public static GravityDirection getPrevGravityDirection(Entity entity) {
        return access(entity).anthology$getPrevGravityDirection();
    }

    public static boolean isGravityForced(Entity entity) {
        return access(entity).anthology$isGravityForced();
    }

    public static boolean isGravityInverted(Entity entity) {
        return getGravityDirection(entity).isInverted();
    }

    public static int getTransitionDuration(Entity entity) {
        return access(entity).anthology$getGravityTransitionDuration();
    }

    public static int getTransitionRemaining(Entity entity) {
        return access(entity).anthology$getGravityTransitionRemaining();
    }

    public static float getGravityFlipProgress(Entity entity, float partialTick) {
        GravityAccess access = access(entity);
        GravityDirection current = access.anthology$getGravityDirection();
        GravityDirection previous = access.anthology$getPrevGravityDirection();
        int duration = access.anthology$getGravityTransitionDuration();
        int remaining = access.anthology$getGravityTransitionRemaining();
        if (duration <= 0 || current == previous) {
            return current.isInverted() ? 1.0F : 0.0F;
        }

        float elapsed = duration - remaining + partialTick;
        float fraction = Math.max(0.0F, Math.min(1.0F, elapsed / duration));
        return previous.isInverted() ? 1.0F - fraction : fraction;
    }

    public static void setGravityDirection(Entity entity, GravityDirection direction) {
        setGravityDirection(entity, direction, false, GravityTransition.INSTANT);
    }

    public static void setGravityDirection(Entity entity, GravityDirection direction, GravityTransition transition) {
        setGravityDirection(entity, direction, false, transition);
    }

    public static void setGravityDirection(Entity entity, GravityDirection direction, boolean forced) {
        setGravityDirection(entity, direction, forced, GravityTransition.INSTANT);
    }

    public static void setGravityDirection(Entity entity, GravityDirection direction, boolean forced, GravityTransition transition) {
        access(entity).anthology$setEffectGravityManaged(false, GravityDirection.DOWN);
        access(entity).anthology$setGravityState(direction, forced, transition);
    }

    public static void setForcedGravityDirection(Entity entity, GravityDirection direction) {
        setForcedGravityDirection(entity, direction, GravityTransition.INSTANT);
    }

    public static void setForcedGravityDirection(Entity entity, GravityDirection direction, GravityTransition transition) {
        access(entity).anthology$setEffectGravityManaged(false, GravityDirection.DOWN);
        access(entity).anthology$setGravityState(direction, true, transition);
    }

    public static void setAirborneGravityDirection(Entity entity, GravityDirection direction, boolean forced, GravityTransition transition) {
        access(entity).anthology$setEffectGravityManaged(false, GravityDirection.DOWN);
        access(entity).anthology$setAirborneGravityState(direction, forced, transition);
    }

    public static void clearForcedGravity(Entity entity) {
        access(entity).anthology$setEffectGravityManaged(false, GravityDirection.DOWN);
        access(entity).anthology$setGravityState(GravityDirection.DOWN, false, GravityTransition.INSTANT);
    }

    public static void applySyncedState(
            Entity entity,
            GravityDirection direction,
            GravityDirection previousDirection,
            boolean forced,
            int transitionDuration,
            int transitionRemaining
    ) {
        access(entity).anthology$applySyncedGravityState(direction, previousDirection, forced, transitionDuration, transitionRemaining);
    }

    public static void resetGravityState(Entity entity) {
        access(entity).anthology$resetGravityState();
    }

    public static void normalizeProjectileFrame(Projectile projectile) {
        if (getGravityDirection(projectile) != GravityDirection.DOWN || isGravityForced(projectile)) {
            setAirborneGravityDirection(projectile, GravityDirection.DOWN, false, GravityTransition.INSTANT);
        }
    }

    public static void notifyGravityStateChanged(Entity entity) {
        GravitySync.syncEntity(entity);
    }

    public static Vec3 getWorldVelocity(Entity entity) {
        return GravityRotationUtil.vecPlayerToWorld(entity.getDeltaMovement(), getGravityDirection(entity));
    }

    public static void setWorldVelocity(Entity entity, Vec3 worldVelocity) {
        entity.setDeltaMovement(GravityRotationUtil.vecWorldToPlayer(worldVelocity, getGravityDirection(entity)));
    }

    public static Vec3 getEyeOffset(Entity entity) {
        return GravityRotationUtil.getEyeOffset(entity, entity.getEyeHeight());
    }

    public static double eyeX(Entity entity) {
        return isGravityInverted(entity) ? entity.getEyePosition().x : entity.getX();
    }

    public static double eyeY(Entity entity) {
        return isGravityInverted(entity) ? entity.getEyePosition().y : entity.getY();
    }

    public static double eyeZ(Entity entity) {
        return isGravityInverted(entity) ? entity.getEyePosition().z : entity.getZ();
    }

    public static Vec3 deltaMovement(net.minecraft.world.entity.LivingEntity target) {
        return isGravityInverted(target) ? getWorldVelocity(target) : target.getDeltaMovement();
    }

    public static double projectileSpawnX(net.minecraft.world.entity.LivingEntity shooter) {
        return projectileSpawnVec(shooter).x;
    }

    public static double projectileSpawnY(net.minecraft.world.entity.LivingEntity shooter) {
        return projectileSpawnVec(shooter).y;
    }

    public static double projectileSpawnZ(net.minecraft.world.entity.LivingEntity shooter) {
        return projectileSpawnVec(shooter).z;
    }

    public static double rangedBodyTargetX(net.minecraft.world.entity.LivingEntity target) {
        if (!isGravityInverted(target)) {
            return target.getX();
        }
        return target.position().add(GravityRotationUtil.vecPlayerToWorld(
                0.0D,
                target.getBbHeight() * 0.3333333333333333D,
                0.0D,
                getGravityDirection(target)
        )).x;
    }

    public static double rangedBodyTargetY(net.minecraft.world.entity.LivingEntity target, double heightScale) {
        if (!isGravityInverted(target)) {
            return target.getY(heightScale);
        }
        return target.position().add(GravityRotationUtil.vecPlayerToWorld(
                0.0D,
                target.getBbHeight() * 0.3333333333333333D,
                0.0D,
                getGravityDirection(target)
        )).y;
    }

    public static double rangedBodyTargetZ(net.minecraft.world.entity.LivingEntity target) {
        if (!isGravityInverted(target)) {
            return target.getZ();
        }
        return target.position().add(GravityRotationUtil.vecPlayerToWorld(
                0.0D,
                target.getBbHeight() * 0.3333333333333333D,
                0.0D,
                getGravityDirection(target)
        )).z;
    }

    public static double rangedEyeTargetX(net.minecraft.world.entity.LivingEntity target) {
        if (!isGravityInverted(target)) {
            return target.getX();
        }
        return target.position().add(GravityRotationUtil.vecPlayerToWorld(
                0.0D,
                target.getEyeHeight() - 1.100000023841858D,
                0.0D,
                getGravityDirection(target)
        )).x;
    }

    public static double rangedEyeTargetY(net.minecraft.world.entity.LivingEntity target) {
        if (!isGravityInverted(target)) {
            return target.getEyeY();
        }
        return target.position().add(GravityRotationUtil.vecPlayerToWorld(
                0.0D,
                target.getEyeHeight() - 1.100000023841858D,
                0.0D,
                getGravityDirection(target)
        )).y + 1.100000023841858D;
    }

    public static double rangedEyeTargetZ(net.minecraft.world.entity.LivingEntity target) {
        if (!isGravityInverted(target)) {
            return target.getZ();
        }
        return target.position().add(GravityRotationUtil.vecPlayerToWorld(
                0.0D,
                target.getEyeHeight() - 1.100000023841858D,
                0.0D,
                getGravityDirection(target)
        )).z;
    }

    public static double rangedSqrt(double value, net.minecraft.world.entity.LivingEntity target) {
        return isGravityInverted(target) ? Math.sqrt(Math.sqrt(value)) : Math.sqrt(value);
    }

    private static GravityAccess access(Entity entity) {
        if (entity instanceof GravityAccess access) {
            return access;
        }

        throw new IllegalStateException("Entity " + entity + " is missing Anthology gravity state");
    }

    private static Vec3 projectileSpawnVec(net.minecraft.world.entity.LivingEntity shooter) {
        GravityDirection direction = getGravityDirection(shooter);
        if (!direction.isInverted()) {
            return new Vec3(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
        }
        return shooter.getEyePosition().subtract(GravityRotationUtil.vecPlayerToWorld(0.0D, 0.1D, 0.0D, direction));
    }
}
