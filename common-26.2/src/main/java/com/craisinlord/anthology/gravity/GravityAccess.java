package com.craisinlord.anthology.gravity;

public interface GravityAccess {
    GravityDirection anthology$getGravityDirection();

    GravityDirection anthology$getPrevGravityDirection();

    boolean anthology$isGravityForced();

    boolean anthology$isEffectGravityManaged();

    GravityDirection anthology$getGravityBeforeEffect();

    void anthology$setEffectGravityManaged(boolean managed, GravityDirection previousDirection);
    int anthology$getGravityTransitionDuration();

    int anthology$getGravityTransitionRemaining();

    void anthology$setGravityState(GravityDirection direction, boolean forced, GravityTransition transition);

    void anthology$setAirborneGravityState(GravityDirection direction, boolean forced, GravityTransition transition);

    void anthology$applySyncedGravityState(
            GravityDirection direction,
            GravityDirection previousDirection,
            boolean forced,
            int transitionDuration,
            int transitionRemaining
    );

    void anthology$resetGravityState();
}
