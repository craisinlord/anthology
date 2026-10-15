package com.craisinlord.anthology.compat.irons_spellbooks;

import com.craisinlord.anthology.portal.PortalEntity;
import com.craisinlord.anthology.portal.PortalRaycast;
import com.craisinlord.anthology.portal.PortalTransitMath;
import com.craisinlord.anthology.time.TimeDilationFieldEntity;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class IronsSpellbooksCompat {
    private static final String MARKER = "io/redspace/ironsspellbooks/entity/mobs/IMagicSummon.class";
    private static final double FIELD_SEARCH_RADIUS = 32.0D;
    private static final ThreadLocal<Integer> PORTAL_OPAQUE_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<RayPath> LAST_RAY = new ThreadLocal<>();
    private static final Map<Entity, PendingLook> PENDING_LOOKS = new WeakHashMap<>();

    public record RayPath(Vec3 start, List<PortalRaycast.Crossing> crossings, Vec3 end) {
    }

    private record PendingLook(Vec3 destination, Vec3 worldLook) {
    }

    private IronsSpellbooksCompat() {
    }

    public static void init() {
        ClassLoader loader = IronsSpellbooksCompat.class.getClassLoader();
        if (loader == null || loader.getResource(MARKER) == null) {
            return;
        }
        IronsSpellbooksSummonRates.register();
    }

    public static boolean isPortalOpaque() {
        return PORTAL_OPAQUE_DEPTH.get() > 0;
    }

    public static void enterPortalOpaque() {
        PORTAL_OPAQUE_DEPTH.set(PORTAL_OPAQUE_DEPTH.get() + 1);
    }

    public static void exitPortalOpaque() {
        int depth = PORTAL_OPAQUE_DEPTH.get() - 1;
        if (depth <= 0) {
            PORTAL_OPAQUE_DEPTH.remove();
        } else {
            PORTAL_OPAQUE_DEPTH.set(depth);
        }
    }

    public static void recordRay(Vec3 start, List<PortalRaycast.Crossing> crossings, Vec3 end) {
        LAST_RAY.set(new RayPath(start, List.copyOf(crossings), end));
    }

    public static RayPath consumeRay() {
        RayPath path = LAST_RAY.get();
        LAST_RAY.remove();
        return path;
    }

    public static void setPortalLook(Entity entity, Vec3 destination, Vec3 worldLook) {
        PENDING_LOOKS.put(entity, new PendingLook(destination, worldLook));
    }

    public static void clearPortalLook(Entity entity) {
        PENDING_LOOKS.remove(entity);
    }

    public static void applyPortalLook(Entity entity, Vec3 destination) {
        PendingLook pending = PENDING_LOOKS.remove(entity);
        if (pending == null || pending.destination().distanceToSqr(destination) > 1.0E-4D) {
            return;
        }
        Vec3 look = PortalTransitMath.entityVelocityFromWorld(entity, pending.worldLook());
        float yaw = yawOf(look);
        float pitch = pitchOf(look);
        if (entity instanceof ServerPlayer player) {
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), yaw, pitch);
            return;
        }
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.yRotO = yaw;
        entity.xRotO = pitch;
        entity.setYHeadRot(yaw);
        if (entity instanceof LivingEntity living) {
            living.setYBodyRot(yaw);
        }
    }

    public static List<Entity> splitRayVisual(Level level, Entity visual, LivingEntity caster, RayPath path) {
        if (path == null || path.crossings().isEmpty()) {
            return null;
        }
        try {
            Constructor<? extends Entity> constructor = visual.getClass().getConstructor(Level.class, Vec3.class, Vec3.class, LivingEntity.class);
            Vec3 offset = constructor.newInstance(level, Vec3.ZERO, new Vec3(0.0D, 0.0D, 1.0D), caster).position();
            Vec3 start = visual.position().subtract(offset);
            List<Entity> segments = new ArrayList<>();
            segments.add(constructor.newInstance(level, start, path.crossings().getFirst().entry(), caster));
            for (int i = 0; i < path.crossings().size(); i++) {
                PortalRaycast.Crossing crossing = path.crossings().get(i);
                Vec3 from = crossing.exit();
                Vec3 to = i + 1 < path.crossings().size() ? path.crossings().get(i + 1).entry() : path.end();
                Entity segment = constructor.newInstance(level, from.subtract(offset), to.subtract(offset), caster);
                Vec3 direction = to.subtract(from);
                if (direction.lengthSqr() > 1.0E-8D) {
                    float yaw = yawOf(direction);
                    float pitch = pitchOf(direction);
                    segment.setYRot(yaw);
                    segment.setXRot(pitch);
                    segment.yRotO = yaw;
                    segment.xRotO = pitch;
                }
                segments.add(segment);
            }
            return segments;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    public static void dispelAlongRay(Level level, LivingEntity caster, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-6D) {
            return;
        }
        double bestDistance = Double.MAX_VALUE;
        Runnable best = null;
        for (PortalEntity portal : PortalEntity.findPortalsNearBounds(level, new AABB(start, end).inflate(0.5D))) {
            double t = PortalRaycast.intersect(portal.getWorldPortalShape(), start, end);
            if (t >= 0.0D && portal.canBeDispelled() && t * length < bestDistance) {
                bestDistance = t * length;
                best = portal::dispel;
            }
        }
        AABB search = new AABB(start, end).inflate(FIELD_SEARCH_RADIUS);
        for (TimeDilationFieldEntity field : level.getEntitiesOfClass(TimeDilationFieldEntity.class, search)) {
            if (field.isRemoved() || !field.canBeDispelled() || field.isOwnedBy(caster)) {
                continue;
            }
            double distance = sphereEntry(start, delta, length, field.position(), field.fieldRadius());
            if (distance >= 0.0D && distance < bestDistance) {
                bestDistance = distance;
                best = field::dispel;
            }
        }
        if (best != null) {
            best.run();
        }
    }

    private static double sphereEntry(Vec3 start, Vec3 delta, double length, Vec3 center, double radius) {
        Vec3 direction = delta.scale(1.0D / length);
        Vec3 offset = start.subtract(center);
        double c = offset.lengthSqr() - radius * radius;
        if (c <= 0.0D) {
            return 0.0D;
        }
        double b = offset.dot(direction);
        double discriminant = b * b - c;
        if (b > 0.0D || discriminant < 0.0D) {
            return -1.0D;
        }
        double distance = -b - Math.sqrt(discriminant);
        return distance <= length ? distance : -1.0D;
    }

    private static float yawOf(Vec3 direction) {
        return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
    }

    private static float pitchOf(Vec3 direction) {
        Vec3 normalized = direction.normalize();
        return (float) Math.toDegrees(Math.asin(Math.max(-1.0D, Math.min(1.0D, -normalized.y))));
    }
}
