package com.craisinlord.anthology.portal;

public interface PortalSettings {
    PortalSettings DEFAULT = new PortalSettings() {
    };

    default boolean seeThroughEnabled() {
        return true;
    }

    default int maxLiveViews() {
        return 8;
    }

    default double minFloorExitSpeed() {
        return 0.6D;
    }

    default double maxExitSpeed() {
        return 0.0D;
    }

    default boolean fancySurfaces() {
        return true;
    }
}
