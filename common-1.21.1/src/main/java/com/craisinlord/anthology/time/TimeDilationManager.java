package com.craisinlord.anthology.time;

import com.craisinlord.anthology.effect.AnthologyEffects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import com.craisinlord.anthology.effect.DilatedMobEffect;
import com.craisinlord.anthology.effect.ContractedMobEffect;

public final class TimeDilationManager {
    private static final Map<ServerLevel, TrackingState> TRACKING = new WeakHashMap<>();
    private static final Map<ServerLevel, Set<TimeDilationFieldEntity>> ACTIVE_FIELDS = new WeakHashMap<>();

    private TimeDilationManager() {
    }

    public static void tickServer(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            Set<TimeDilationFieldEntity> activeFields = ACTIVE_FIELDS.get(level);
            if (activeFields != null) {
                activeFields.removeIf(field -> !field.isAlive() || field.isRemoved());
                if (activeFields.isEmpty()) {
                    ACTIVE_FIELDS.remove(level);
                    activeFields = null;
                }
            }
            TrackingState tracking = TRACKING.get(level);
            for (ServerPlayer player : level.players()) {
                trackPotentiallyAffected(player);
            }
            if (activeFields == null && (tracking == null || tracking.entities.isEmpty())) {
                continue;
            }
            List<TimeDilationFieldEntity> fields = activeFields == null ? List.of() : List.copyOf(activeFields);
            if (tracking == null) {
                tracking = TRACKING.computeIfAbsent(level, ignored -> new TrackingState());
            }
            discoverFieldEntities(level, fields, tracking);
            syncFieldSnapshots(level, fields, tracking);
            updateEntities(level, fields, tracking);
            if (fields.isEmpty() && tracking.entities.isEmpty()) {
                TRACKING.remove(level);
            }
            if (!fields.isEmpty()) {
                TimeDilationParticles.spawnFieldBorders(level, fields.stream().filter(TimeDilationFieldEntity::isVisual).toList());
            }
        }
    }

    public static void registerActiveField(TimeDilationFieldEntity field) {
        if (field.level() instanceof ServerLevel level) {
            ACTIVE_FIELDS.computeIfAbsent(level, ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(field);
        }
    }

    public static void trackPotentiallyAffected(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level) || entity instanceof TimeDilationFieldEntity) {
            return;
        }
        boolean hookTracked = TimeDilationHooks.isTracked(entity);
        boolean hasEffect = entity instanceof net.minecraft.world.entity.LivingEntity living
                && ((AnthologyEffects.dilated() != null && living.hasEffect(AnthologyEffects.dilated()))
                || (AnthologyEffects.contracted() != null && living.hasEffect(AnthologyEffects.contracted())));
        if (entity instanceof TimeDilationEntityAccess access
                && Math.abs(access.anthology$getTimeDilationRate() - TimeDilationMath.NORMAL_RATE) < 0.001D
                && Math.abs(access.anthology$getInheritedTimeDilationRate() - TimeDilationMath.NORMAL_RATE) < 0.001D
                && Math.abs(TimeDilationApi.getVehicleRate(entity) - TimeDilationMath.NORMAL_RATE) < 0.001D
                && !TimeDilationApi.hasPersonalRates(entity)
                && !hookTracked && !hasEffect) {
            return;
        }
        TRACKING.computeIfAbsent(level, ignored -> new TrackingState()).entities.add(entity);
    }

    public static void resyncPersistentEffects(ServerPlayer player) {
        if (!(player instanceof TimeDilationEntityAccess access)) {
            return;
        }

        double targetRate = TimeDilationMath.NORMAL_RATE;
        if (AnthologyEffects.dilated() != null) {
            var effect = player.getEffect(AnthologyEffects.dilated());
            if (effect != null) {
                targetRate = Math.min(targetRate, DilatedMobEffect.rateForAmplifier(effect.getAmplifier()));
            }
        }
        if (AnthologyEffects.contracted() != null) {
            var effect = player.getEffect(AnthologyEffects.contracted());
            if (effect != null && targetRate >= TimeDilationMath.NORMAL_RATE) {
                targetRate = Math.max(targetRate, ContractedMobEffect.rateForAmplifier(effect.getAmplifier()));
            }
        }

        if (Math.abs(targetRate - TimeDilationMath.NORMAL_RATE) >= 0.001D) {
            access.anthology$setTimeDilationRate(targetRate);
            TimeDilationApi.syncEntityRate(player, targetRate);
        }
        trackPotentiallyAffected(player);
    }

    private static void syncFieldSnapshots(ServerLevel level, List<TimeDilationFieldEntity> fields, TrackingState tracking) {
        List<TimeDilationFieldSnapshot> snapshots = fields.stream()
                .filter(field -> !field.isChronosphere())
                .map(field -> new TimeDilationFieldSnapshot(
                        field.getX(), field.getY(), field.getZ(), field.fieldRadius(), field.fieldRate(),
                        field.fieldAge(), field.fieldDurationTicks()))
                .limit(128)
                .toList();
        if (level.getGameTime() % 4L != 0L && snapshots.size() == tracking.lastSnapshots.size()) {
            return;
        }
        tracking.lastSnapshots = snapshots;
        for (ServerPlayer player : level.players()) {
            TimeDilationApi.syncFields(player, snapshots);
        }
    }

    private static void discoverFieldEntities(ServerLevel level, List<TimeDilationFieldEntity> fields, TrackingState tracking) {
        for (TimeDilationFieldEntity field : fields) {
            double radius = field.influenceRadius();
            var center = field.position();
            var area = new net.minecraft.world.phys.AABB(
                    center.x - radius, center.y - radius, center.z - radius,
                    center.x + radius, center.y + radius, center.z + radius);
            tracking.entities.addAll(level.getEntities(field, area,
                    entity -> entity.isAlive() && !(entity instanceof TimeDilationFieldEntity)
                            && entity instanceof TimeDilationEntityAccess));
        }
    }

    private static void updateEntities(ServerLevel level, List<TimeDilationFieldEntity> fields, TrackingState tracking) {
        var iterator = tracking.entities.iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (!(entity instanceof TimeDilationEntityAccess access) || entity instanceof TimeDilationFieldEntity) {
                continue;
            }
            if (entity.getType().is(TimeDilationTags.TIME_DILATION_IMMUNE)) {
                if (AnthologyEffects.dilated() != null && entity instanceof net.minecraft.world.entity.LivingEntity living) {
                    living.removeEffect(AnthologyEffects.dilated());
                }
                TimeDilationApi.clearRate(entity);
                iterator.remove();
                continue;
            }
            double previousRate = access.anthology$getTimeDilationRate();
            double fieldRate = TimeDilationFieldSampler.sample(fields, entity);
            double effectRate = TimeDilationMath.NORMAL_RATE;
            if (AnthologyEffects.dilated() != null
                    && entity instanceof net.minecraft.world.entity.LivingEntity living) {
                var effect = living.getEffect(AnthologyEffects.dilated());
                if (effect != null) {
                    effectRate = DilatedMobEffect.rateForAmplifier(effect.getAmplifier());
                }
            }
            if (AnthologyEffects.contracted() != null
                    && entity instanceof net.minecraft.world.entity.LivingEntity living) {
                var effect = living.getEffect(AnthologyEffects.contracted());
                if (effect != null) {
                    effectRate = Math.max(effectRate, ContractedMobEffect.rateForAmplifier(effect.getAmplifier()));
                }
            }
            double inheritedRate = access.anthology$getInheritedTimeDilationRate();
            double vehicleRate = TimeDilationApi.getVehicleRate(entity);
            if (vehicleRate < TimeDilationMath.NORMAL_RATE) {
                inheritedRate = Math.min(inheritedRate, vehicleRate);
            } else {
                inheritedRate = Math.max(inheritedRate, vehicleRate);
            }
            if (entity instanceof Projectile projectile && projectile.getOwner() != null) {
                double ownerRate = TimeDilationApi.getRate(projectile.getOwner());
                inheritedRate = ownerRate < TimeDilationMath.NORMAL_RATE
                        ? Math.min(inheritedRate, ownerRate)
                        : Math.max(inheritedRate, ownerRate);
            }
            double slowRate = TimeDilationMath.NORMAL_RATE;
            double fastRate = TimeDilationMath.NORMAL_RATE;
            if (fieldRate < TimeDilationMath.NORMAL_RATE) {
                slowRate = Math.min(slowRate, fieldRate);
            } else {
                fastRate = Math.max(fastRate, fieldRate);
            }
            if (effectRate < TimeDilationMath.NORMAL_RATE) {
                slowRate = Math.min(slowRate, effectRate);
            } else {
                fastRate = Math.max(fastRate, effectRate);
            }
            for (TimeDilationHooks.RateSource source : TimeDilationHooks.rateSources()) {
                double sourceRate = source.rate(entity);
                if (sourceRate < TimeDilationMath.NORMAL_RATE) {
                    slowRate = Math.min(slowRate, sourceRate);
                } else {
                    fastRate = Math.max(fastRate, sourceRate);
                }
            }
            if (inheritedRate < TimeDilationMath.NORMAL_RATE) {
                slowRate = Math.min(slowRate, inheritedRate);
            } else {
                fastRate = Math.max(fastRate, inheritedRate);
            }
            for (double personalRate : access.anthology$getPersonalTimeDilationRates().values()) {
                if (personalRate < TimeDilationMath.NORMAL_RATE) {
                    slowRate = Math.min(slowRate, personalRate);
                } else {
                    fastRate = Math.max(fastRate, personalRate);
                }
            }
            double ceiling = access.anthology$getTimeDilationCeiling();
            double targetRate = TimeDilationMath.clampRate(slowRate < TimeDilationMath.NORMAL_RATE ? slowRate : fastRate, ceiling);
            double rate = TimeDilationMath.transitionRate(previousRate, targetRate, ceiling);
            if (Math.abs(targetRate - TimeDilationMath.NORMAL_RATE) < 0.001D
                    && Math.abs(rate - TimeDilationMath.NORMAL_RATE) < 0.001D) {
                TimeDilationApi.clearRate(entity);
            } else {
                access.anthology$setTimeDilationRate(rate);
            }
            if (Math.abs(previousRate - rate) > 0.001D) {
                TimeDilationApi.syncEntityRate(entity, rate);
            }

            boolean hasDilationEffect = entity instanceof net.minecraft.world.entity.LivingEntity living
                    && ((AnthologyEffects.dilated() != null && living.hasEffect(AnthologyEffects.dilated()))
                    || (AnthologyEffects.contracted() != null && living.hasEffect(AnthologyEffects.contracted())));
            boolean hasInheritedRate = Math.abs(access.anthology$getInheritedTimeDilationRate() - TimeDilationMath.NORMAL_RATE) >= 0.001D;
            boolean hasActiveRate = Math.abs(access.anthology$getTimeDilationRate() - TimeDilationMath.NORMAL_RATE) >= 0.001D;
            if (!hasDilationEffect && !hasInheritedRate && !hasActiveRate
                    && Math.abs(targetRate - TimeDilationMath.NORMAL_RATE) < 0.001D) {
                iterator.remove();
            }
        }
        tracking.entities.removeIf(entity -> !entity.isAlive() || entity.isRemoved());
    }

    private static final class TrackingState {
        private final Set<Entity> entities = Collections.newSetFromMap(new IdentityHashMap<>());
        private List<TimeDilationFieldSnapshot> lastSnapshots = List.of();
    }
}
