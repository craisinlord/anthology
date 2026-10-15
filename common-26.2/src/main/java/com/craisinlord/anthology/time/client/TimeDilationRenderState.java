package com.craisinlord.anthology.time.client;

import java.util.List;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public interface TimeDilationRenderState {
    boolean anthology$isTimeDilatedHurt();

    void anthology$setTimeDilatedHurt(boolean value);

    List<Afterimage> anthology$getAfterimages();

    void anthology$setAfterimages(List<Afterimage> afterimages);

    record Afterimage(LivingEntityRenderState state, double dx, double dy, double dz, int color) {
    }
}
