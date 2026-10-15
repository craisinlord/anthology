package com.craisinlord.anthology.portal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class PortalRegistry {
    private static final Map<Level, Map<UUID, PortalEntity>> LEVELS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final AtomicInteger TRACKED = new AtomicInteger();

    private PortalRegistry() {
    }

    static void track(PortalEntity portal) {
        Level level = portal.level();
        if (level == null) {
            return;
        }
        Map<UUID, PortalEntity> portals;
        synchronized (LEVELS) {
            portals = LEVELS.computeIfAbsent(level, ignored -> new ConcurrentHashMap<>());
        }
        if (portals.put(portal.getUUID(), portal) == null) {
            TRACKED.incrementAndGet();
        }
    }

    static void untrack(PortalEntity portal) {
        Map<UUID, PortalEntity> portals = portalsIn(portal.level());
        if (portals != null && portals.remove(portal.getUUID(), portal)) {
            TRACKED.decrementAndGet();
        }
    }

    public static boolean hasAny(Level level) {
        if (TRACKED.get() <= 0) {
            return false;
        }
        Map<UUID, PortalEntity> portals = portalsIn(level);
        return portals != null && !portals.isEmpty();
    }

    public static Collection<PortalEntity> all(Level level) {
        if (TRACKED.get() <= 0) {
            return List.of();
        }
        Map<UUID, PortalEntity> portals = portalsIn(level);
        return portals == null ? List.of() : portals.values();
    }

    public static PortalEntity find(Level level, UUID portalId) {
        if (portalId == null) {
            return null;
        }
        Map<UUID, PortalEntity> portals = portalsIn(level);
        if (portals == null) {
            return null;
        }
        PortalEntity portal = portals.get(portalId);
        return portal != null && portal.isAlive() ? portal : null;
    }

    public static List<PortalEntity> near(Level level, AABB bounds) {
        if (TRACKED.get() <= 0) {
            return List.of();
        }
        Map<UUID, PortalEntity> portals = portalsIn(level);
        if (portals == null || portals.isEmpty()) {
            return List.of();
        }
        List<PortalEntity> nearby = null;
        for (PortalEntity portal : portals.values()) {
            if (portal.isAlive() && portal.getPortalInsides().intersects(bounds)) {
                if (nearby == null) {
                    nearby = new ArrayList<>(2);
                }
                nearby.add(portal);
            }
        }
        return nearby == null ? List.of() : nearby;
    }

    private static Map<UUID, PortalEntity> portalsIn(Level level) {
        if (level == null) {
            return null;
        }
        synchronized (LEVELS) {
            return LEVELS.get(level);
        }
    }
}
