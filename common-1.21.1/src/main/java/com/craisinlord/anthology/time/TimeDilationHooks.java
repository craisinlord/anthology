package com.craisinlord.anthology.time;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class TimeDilationHooks {
    @FunctionalInterface
    public interface RateSource {
        double rate(Entity entity);
    }

    @FunctionalInterface
    public interface FieldExclusion {
        boolean excludes(TimeDilationFieldEntity field, Entity entity);
    }

    private static final List<RateSource> RATE_SOURCES = new CopyOnWriteArrayList<>();
    private static final List<Predicate<Entity>> TRACKED = new CopyOnWriteArrayList<>();
    private static final List<FieldExclusion> FIELD_EXCLUSIONS = new CopyOnWriteArrayList<>();
    private static final List<ToIntFunction<LivingEntity>> CHRONOSPHERE_LEVELS = new CopyOnWriteArrayList<>();

    private TimeDilationHooks() {
    }

    public static void addRateSource(RateSource source) {
        RATE_SOURCES.add(source);
    }

    public static void addTrackedPredicate(Predicate<Entity> predicate) {
        TRACKED.add(predicate);
    }

    public static void addFieldExclusion(FieldExclusion exclusion) {
        FIELD_EXCLUSIONS.add(exclusion);
    }

    public static void addChronosphereLevelSource(ToIntFunction<LivingEntity> source) {
        CHRONOSPHERE_LEVELS.add(source);
    }

    public static List<RateSource> rateSources() {
        return RATE_SOURCES;
    }

    public static boolean isTracked(Entity entity) {
        for (Predicate<Entity> predicate : TRACKED) {
            if (predicate.test(entity)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isExcluded(TimeDilationFieldEntity field, Entity entity) {
        for (FieldExclusion exclusion : FIELD_EXCLUSIONS) {
            if (exclusion.excludes(field, entity)) {
                return true;
            }
        }
        return false;
    }

    public static int chronosphereLevel(LivingEntity entity) {
        int level = 0;
        for (ToIntFunction<LivingEntity> source : CHRONOSPHERE_LEVELS) {
            level = Math.max(level, source.applyAsInt(entity));
        }
        return level;
    }
}
