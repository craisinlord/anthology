package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.TimeDilationEntityAccess;
import com.craisinlord.anthology.time.TimeDilationMath;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityTimeDilationStateMixin implements TimeDilationEntityAccess {
    @Unique
    private double anthology$timeDilationRate = TimeDilationMath.NORMAL_RATE;
    @Unique
    private double anthology$inheritedTimeDilationRate = TimeDilationMath.NORMAL_RATE;
    @Unique
    private final Map<String, Double> anthology$timeDilationTimerProgress = new HashMap<>();
    @Unique
    private boolean anthology$inTimeDilationMove;
    @Unique
    private boolean anthology$applyingExternalImpulse;
    @Unique
    private double anthology$timeDilationCeiling = TimeDilationMath.MAX_RATE;
    @Unique
    private final Map<String, Double> anthology$personalTimeDilationRates = new HashMap<>();

    @Override
    public double anthology$getTimeDilationCeiling() {
        return this.anthology$timeDilationCeiling;
    }

    @Override
    public void anthology$setTimeDilationCeiling(double ceiling) {
        this.anthology$timeDilationCeiling = TimeDilationMath.clampCeiling(ceiling);
    }

    @Override
    public Map<String, Double> anthology$getPersonalTimeDilationRates() {
        return this.anthology$personalTimeDilationRates;
    }

    @Override
    public double anthology$getTimeDilationRate() {
        return this.anthology$timeDilationRate;
    }

    @Override
    public void anthology$setTimeDilationRate(double rate) {
        double previousRate = this.anthology$timeDilationRate;
        this.anthology$timeDilationRate = TimeDilationMath.clampRate(rate, TimeDilationMath.ABSOLUTE_MAX_RATE);
        if (previousRate != TimeDilationMath.NORMAL_RATE
                && this.anthology$timeDilationRate == TimeDilationMath.NORMAL_RATE) {
            this.anthology$timeDilationTimerProgress.clear();
        }
    }

    @Override
    public double anthology$getInheritedTimeDilationRate() {
        return this.anthology$inheritedTimeDilationRate;
    }

    @Override
    public void anthology$setInheritedTimeDilationRate(double rate) {
        this.anthology$inheritedTimeDilationRate = TimeDilationMath.clampRate(rate);
    }

    @Override
    public boolean anthology$consumeTimeDilationTick(String timerKey, double rate) {
        return this.anthology$consumeTimeDilationTicks(timerKey, rate) > 0;
    }

    @Override
    public int anthology$consumeTimeDilationTicks(String timerKey, double rate) {
        double clampedRate = TimeDilationMath.clampRate(rate, TimeDilationMath.ABSOLUTE_MAX_RATE);
        if (Math.abs(clampedRate - TimeDilationMath.NORMAL_RATE) < 0.001D) {
            return 1;
        }

        double progress = this.anthology$timeDilationTimerProgress.getOrDefault(timerKey, 0.0D) + clampedRate;
        int wholeTicks = (int) Math.floor(progress + 1.0E-9D);
        if (wholeTicks <= 0) {
            this.anthology$timeDilationTimerProgress.put(timerKey, progress);
            return 0;
        }

        this.anthology$timeDilationTimerProgress.put(timerKey, progress - wholeTicks);
        return wholeTicks;
    }

    @Override
    public boolean anthology$isInTimeDilationMove() {
        return this.anthology$inTimeDilationMove;
    }

    @Override
    public void anthology$setInTimeDilationMove(boolean inMove) {
        this.anthology$inTimeDilationMove = inMove;
    }

    @Override
    public boolean anthology$isApplyingExternalImpulse() {
        return this.anthology$applyingExternalImpulse;
    }

    @Override
    public void anthology$setApplyingExternalImpulse(boolean applying) {
        this.anthology$applyingExternalImpulse = applying;
    }
}
