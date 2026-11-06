package com.craisinlord.anthology.time.client;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.craisinlord.anthology.time.TimeDilationMath;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import net.minecraft.world.entity.Entity;

public class TimeDilationSpeedModifier extends SpeedModifier {
    private final Entity entity;

    public TimeDilationSpeedModifier(Entity entity) {
        super(1.0F);
        this.entity = entity;
    }

    @Override
    public void tick() {
        this.updateSpeed();
        super.tick();
    }

    @Override
    public void setupAnim(float tickDelta) {
        this.updateSpeed();
        super.setupAnim(tickDelta);
    }

    private void updateSpeed() {
        double rate = TimeDilationApi.getRate(this.entity);
        this.speed = (float) Math.max(TimeDilationMath.MIN_RATE, Math.min(TimeDilationMath.ABSOLUTE_MAX_RATE, rate));
    }
}
