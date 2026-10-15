package com.craisinlord.anthology.time;

import java.util.Map;

public interface TimeDilationEntityAccess {
    double anthology$getTimeDilationCeiling();

    void anthology$setTimeDilationCeiling(double ceiling);

    Map<String, Double> anthology$getPersonalTimeDilationRates();

    double anthology$getTimeDilationRate();

    void anthology$setTimeDilationRate(double rate);

    double anthology$getInheritedTimeDilationRate();

    void anthology$setInheritedTimeDilationRate(double rate);

    boolean anthology$consumeTimeDilationTick(String timerKey, double rate);

    int anthology$consumeTimeDilationTicks(String timerKey, double rate);

    boolean anthology$isInTimeDilationMove();

    void anthology$setInTimeDilationMove(boolean inMove);

    boolean anthology$isApplyingExternalImpulse();

    void anthology$setApplyingExternalImpulse(boolean applying);

    double anthology$getDilatedTickCount();

    void anthology$setDilatedTickCount(double tickCount);

    int anthology$getDilatedTickStamp();

    void anthology$setDilatedTickStamp(int stamp);
}
