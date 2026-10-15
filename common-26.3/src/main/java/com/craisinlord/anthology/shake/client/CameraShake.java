package com.craisinlord.anthology.shake.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class CameraShake {
    @FunctionalInterface
    public interface StrengthSource {
        float strength(Vec3 cameraPos);
    }

    public record Angles(float yaw, float pitch, float roll) {
    }

    private record Source(double range, float strength, BooleanSupplier active) {
    }

    private record Impact(Vec3 position, float intensity, int remainingTicks, int totalTicks, float radius) {
        private Impact ticked() {
            return new Impact(position, intensity, remainingTicks - 1, totalTicks, radius);
        }
    }

    private static final Map<LivingEntity, Source> SOURCES = new HashMap<>();
    private static final List<Impact> IMPACTS = new ArrayList<>();
    private static final List<StrengthSource> STRENGTH_SOURCES = new CopyOnWriteArrayList<>();

    private CameraShake() {
    }

    public static void register(LivingEntity entity, double range, float strength, BooleanSupplier active) {
        SOURCES.putIfAbsent(entity, new Source(range, strength, active));
    }

    public static void addStrengthSource(StrengthSource source) {
        STRENGTH_SOURCES.add(source);
    }

    public static void triggerImpact(Vec3 position, float intensity, int durationTicks, float radius) {
        int ticks = Math.max(1, durationTicks);
        IMPACTS.add(new Impact(position, intensity, ticks, ticks, Math.max(1.0F, radius)));
    }

    public static float getStrength(Vec3 cameraPos) {
        float total = 0.0F;
        Iterator<Map.Entry<LivingEntity, Source>> iterator = SOURCES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LivingEntity, Source> entry = iterator.next();
            LivingEntity entity = entry.getKey();
            Source source = entry.getValue();
            if (entity.isRemoved() || !entity.isAlive() || !source.active().getAsBoolean()) {
                iterator.remove();
                continue;
            }
            double distance = Math.sqrt(cameraPos.distanceToSqr(entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D)));
            if (distance > source.range()) {
                continue;
            }
            total += (float) ((1.0D - distance / source.range()) * source.strength());
        }
        Iterator<Impact> impactIterator = IMPACTS.iterator();
        while (impactIterator.hasNext()) {
            Impact impact = impactIterator.next();
            if (impact.remainingTicks() <= 0) {
                impactIterator.remove();
                continue;
            }
            double distance = Math.sqrt(cameraPos.distanceToSqr(impact.position()));
            if (distance > impact.radius()) {
                continue;
            }
            total += (float) ((1.0D - distance / impact.radius()) * impact.intensity() * (impact.remainingTicks() / (float) Math.max(1, impact.totalTicks())));
        }
        for (StrengthSource source : STRENGTH_SOURCES) {
            total += source.strength(cameraPos);
        }
        return total;
    }

    public static Angles angles(Vec3 cameraPos, float time) {
        float strength = getStrength(cameraPos);
        if (strength <= 0.0F) {
            return null;
        }
        return new Angles(
                Mth.sin(time * 1.5F) * strength * 2.5F,
                Mth.cos(time * 1.8F) * strength * 2.0F,
                Mth.sin(time * 2.2F) * strength * 1.5F
        );
    }

    public static void tick() {
        for (int i = IMPACTS.size() - 1; i >= 0; i--) {
            Impact impact = IMPACTS.get(i);
            if (impact.remainingTicks() <= 1) {
                IMPACTS.remove(i);
            } else {
                IMPACTS.set(i, impact.ticked());
            }
        }
    }

    public static void clear() {
        SOURCES.clear();
        IMPACTS.clear();
    }
}
