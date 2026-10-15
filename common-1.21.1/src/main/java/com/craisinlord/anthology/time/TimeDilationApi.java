package com.craisinlord.anthology.time;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class TimeDilationApi {
    private static final Map<UUID, Double> SYNCED_CLIENT_RATES = new ConcurrentHashMap<>();
    private static final ThreadLocal<Integer> ROTATION_BYPASS_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final List<Predicate<Entity>> HORIZONTAL_ONLY_MOVEMENT = new CopyOnWriteArrayList<>();

    private TimeDilationApi() {
    }

    public static TimeDilationFieldEntity createField(ServerLevel level, Vec3 center, double radius, double rate, int durationTicks) {
        return createField(level, center, radius, rate, durationTicks, null);
    }

    public static TimeDilationFieldEntity createField(ServerLevel level, Vec3 center, double radius, double rate, int durationTicks, UUID ownerId) {
        return createField(level, center, radius, rate, durationTicks, ownerId, true);
    }

    public static TimeDilationFieldEntity createField(ServerLevel level, Vec3 center, double radius, double rate, int durationTicks, UUID ownerId, boolean visual) {
        TimeDilationFieldEntity field = TimeDilationFieldEntity.create(level, center, radius, rate, durationTicks, ownerId, visual);
        level.addFreshEntity(field);
        return field;
    }

    public static double getRate(Entity entity) {
        if (entity instanceof TimeDilationEntityAccess access) {
            if (entity.level().isClientSide) {
                Double syncedRate = SYNCED_CLIENT_RATES.get(entity.getUUID());
                if (syncedRate != null) {
                    return syncedRate;
                }
            }
            return access.anthology$getTimeDilationRate();
        }
        return TimeDilationMath.NORMAL_RATE;
    }

    public static boolean isDilated(Entity entity) {
        return getRate(entity) < TimeDilationMath.NORMAL_RATE;
    }

    public static double getVehicleRate(Entity entity) {
        Entity vehicle = entity.getVehicle();
        return vehicle == null ? TimeDilationMath.NORMAL_RATE : getRate(vehicle);
    }

    public static void enterRotationBypass() {
        ROTATION_BYPASS_DEPTH.set(ROTATION_BYPASS_DEPTH.get() + 1);
    }

    public static void exitRotationBypass() {
        int depth = ROTATION_BYPASS_DEPTH.get() - 1;
        if (depth <= 0) {
            ROTATION_BYPASS_DEPTH.remove();
        } else {
            ROTATION_BYPASS_DEPTH.set(depth);
        }
    }

    public static boolean isRotationBypassed() {
        return ROTATION_BYPASS_DEPTH.get() > 0;
    }

    public static void setInheritedRate(Entity entity, double rate) {
        if (entity instanceof TimeDilationEntityAccess access) {
            access.anthology$setInheritedTimeDilationRate(rate);
        }
    }

    public static void applySyncedRate(Entity entity, double rate) {
        applySyncedRate(entity.getUUID(), rate);
        if (entity instanceof TimeDilationEntityAccess access) {
            access.anthology$setTimeDilationRate(rate);
        }
    }

    public static void applySyncedRate(UUID entityUuid, double rate) {
        double clampedRate = TimeDilationMath.clampRate(rate, TimeDilationMath.ABSOLUTE_MAX_RATE);
        SYNCED_CLIENT_RATES.put(entityUuid, clampedRate);
    }

    public static void clearRate(Entity entity) {
        if (entity instanceof TimeDilationEntityAccess access) {
            boolean wasDilated = Math.abs(access.anthology$getTimeDilationRate() - TimeDilationMath.NORMAL_RATE) > 0.001D;
            access.anthology$setTimeDilationRate(TimeDilationMath.NORMAL_RATE);
            if (wasDilated) {
                syncEntityRate(entity, TimeDilationMath.NORMAL_RATE);
            }
        }
        SYNCED_CLIENT_RATES.remove(entity.getUUID());
    }

    public static void clearSyncedClientRates() {
        SYNCED_CLIENT_RATES.clear();
    }

    public static boolean hasSyncedClientRates() {
        return !SYNCED_CLIENT_RATES.isEmpty();
    }

    public static void syncEntityRate(Entity entity, double rate) {
        TimeDilationSync.syncRate(entity, TimeDilationMath.clampRate(rate, TimeDilationMath.ABSOLUTE_MAX_RATE));
    }

    public static void addHorizontalOnlyMovement(Predicate<Entity> predicate) {
        HORIZONTAL_ONLY_MOVEMENT.add(predicate);
    }

    public static boolean isHorizontalOnlyMovement(Entity entity) {
        for (Predicate<Entity> predicate : HORIZONTAL_ONLY_MOVEMENT) {
            if (predicate.test(entity)) {
                return true;
            }
        }
        return false;
    }

    public static double getRateCeiling(Entity entity) {
        if (entity instanceof TimeDilationEntityAccess access) {
            return access.anthology$getTimeDilationCeiling();
        }
        return TimeDilationMath.MAX_RATE;
    }

    public static void setRateCeiling(Entity entity, double ceiling) {
        if (entity instanceof TimeDilationEntityAccess access) {
            access.anthology$setTimeDilationCeiling(ceiling);
            TimeDilationManager.trackPotentiallyAffected(entity);
        }
    }

    public static void clearRateCeiling(Entity entity) {
        setRateCeiling(entity, TimeDilationMath.MAX_RATE);
    }

    public static void setPersonalRate(Entity entity, String sourceId, double rate) {
        if (entity instanceof TimeDilationEntityAccess access) {
            double clampedRate = TimeDilationMath.clampRate(rate, TimeDilationMath.ABSOLUTE_MAX_RATE);
            if (Math.abs(clampedRate - TimeDilationMath.NORMAL_RATE) < 0.001D) {
                access.anthology$getPersonalTimeDilationRates().remove(sourceId);
            } else {
                access.anthology$getPersonalTimeDilationRates().put(sourceId, clampedRate);
            }
            TimeDilationManager.trackPotentiallyAffected(entity);
        }
    }

    public static void clearPersonalRate(Entity entity, String sourceId) {
        if (entity instanceof TimeDilationEntityAccess access
                && access.anthology$getPersonalTimeDilationRates().remove(sourceId) != null) {
            TimeDilationManager.trackPotentiallyAffected(entity);
        }
    }

    public static double getPersonalRate(Entity entity, String sourceId) {
        if (entity instanceof TimeDilationEntityAccess access) {
            return access.anthology$getPersonalTimeDilationRates().getOrDefault(sourceId, TimeDilationMath.NORMAL_RATE);
        }
        return TimeDilationMath.NORMAL_RATE;
    }

    public static boolean hasPersonalRates(Entity entity) {
        return entity instanceof TimeDilationEntityAccess access && !access.anthology$getPersonalTimeDilationRates().isEmpty();
    }

    public static TimeDilationFieldEntity createAttachedField(ServerLevel level, Entity owner, double radius, double rate, int durationTicks) {
        TimeDilationFieldEntity field = TimeDilationFieldEntity.create(level, owner.position(), radius, rate, durationTicks, owner.getUUID(), true);
        field.attachTo(owner);
        level.addFreshEntity(field);
        return field;
    }

    public static void syncFields(ServerPlayer player, List<TimeDilationFieldSnapshot> fields) {
        TimeDilationSync.syncFields(player, fields);
    }

    public static boolean consumeTick(Entity entity, String timerKey) {
        return consumeTicks(entity, timerKey) > 0;
    }

    public static int consumeTicks(Entity entity, String timerKey) {
        double rate = getRate(entity);
        if (entity instanceof TimeDilationEntityAccess access) {
            return access.anthology$consumeTimeDilationTicks(timerKey, rate);
        }
        return 1;
    }

    public static int getDilatedTickCount(Entity entity) {
        if (!(entity instanceof TimeDilationEntityAccess access)) {
            return entity.tickCount;
        }
        double count = access.anthology$getDilatedTickCount();
        int stamp = access.anthology$getDilatedTickStamp();
        if (count < 0.0D || entity.tickCount < stamp) {
            count = entity.tickCount;
        } else if (entity.tickCount > stamp) {
            double rate = Math.max(TimeDilationMath.MIN_RATE, Math.min(TimeDilationMath.ABSOLUTE_MAX_RATE, getRate(entity)));
            count += (entity.tickCount - stamp) * rate;
        }
        access.anthology$setDilatedTickCount(count);
        access.anthology$setDilatedTickStamp(entity.tickCount);
        return (int) Math.floor(count + 1.0E-9D);
    }

    public static void runScaledTick(Entity entity, Runnable tick) {
        int ticks = consumeTicks(entity, "scaled_entity_tick");
        if (ticks == 1) {
            tick.run();
            return;
        }
        entity.tickCount--;
        for (int i = 0; i < ticks && !entity.isRemoved(); i++) {
            entity.tickCount++;
            tick.run();
        }
    }

    public static int scaleCooldownTicks(Entity entity, int ticks) {
        double rate = getRate(entity);
        if (ticks <= 0 || Math.abs(rate - TimeDilationMath.NORMAL_RATE) < 0.001D) {
            return ticks;
        }
        return (int) Math.ceil(ticks / Math.max(TimeDilationMath.MIN_RATE, rate));
    }
}
