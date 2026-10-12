package com.craisinlord.anthology.gravity;

import java.util.Optional;

public enum GravityDirection {
    DOWN(0, "down", false),
    UP(1, "up", true);

    private final int id;
    private final String serializedName;
    private final boolean inverted;

    GravityDirection(int id, String serializedName, boolean inverted) {
        this.id = id;
        this.serializedName = serializedName;
        this.inverted = inverted;
    }

    public int getId() {
        return this.id;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public boolean isInverted() {
        return this.inverted;
    }

    public static GravityDirection byId(int id) {
        return id == UP.id ? UP : DOWN;
    }

    public static Optional<GravityDirection> parse(String value) {
        for (GravityDirection direction : values()) {
            if (direction.serializedName.equalsIgnoreCase(value)) {
                return Optional.of(direction);
            }
        }

        return Optional.empty();
    }
}
