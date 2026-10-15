package com.craisinlord.anthology.gravity;

public record GravityTransition(int durationTicks) {
    public static final GravityTransition INSTANT = new GravityTransition(0);

    public GravityTransition {
        durationTicks = Math.max(0, durationTicks);
    }
}
