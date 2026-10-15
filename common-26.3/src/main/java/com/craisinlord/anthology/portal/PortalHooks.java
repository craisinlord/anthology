package com.craisinlord.anthology.portal;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;

public final class PortalHooks {
    @FunctionalInterface
    public interface TransitListener {
        void onTransit(Entity entity, PortalEntity from, PortalEntity to);
    }

    private static final List<Predicate<Entity>> TRANSIT_EXCLUSIONS = new CopyOnWriteArrayList<>();
    private static final List<TransitListener> TRANSIT_LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile PortalSettings settings = PortalSettings.DEFAULT;

    private PortalHooks() {
    }

    public static void setSettings(PortalSettings newSettings) {
        settings = newSettings == null ? PortalSettings.DEFAULT : newSettings;
    }

    public static PortalSettings settings() {
        return settings;
    }

    public static void addTransitExclusion(Predicate<Entity> exclusion) {
        TRANSIT_EXCLUSIONS.add(exclusion);
    }

    public static void addTransitListener(TransitListener listener) {
        TRANSIT_LISTENERS.add(listener);
    }

    public static boolean isTransitExcluded(Entity entity) {
        for (Predicate<Entity> exclusion : TRANSIT_EXCLUSIONS) {
            if (exclusion.test(entity)) {
                return true;
            }
        }
        return false;
    }

    static void fireTransit(Entity entity, PortalEntity from, PortalEntity to) {
        for (TransitListener listener : TRANSIT_LISTENERS) {
            listener.onTransit(entity, from, to);
        }
    }
}
