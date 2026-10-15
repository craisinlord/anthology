package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.client.TimeDilationRenderState;
import java.util.List;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateTimeDilationMixin implements TimeDilationRenderState {
    @Unique
    private boolean anthology$timeDilatedHurt;

    @Unique
    private List<Afterimage> anthology$afterimages = List.of();

    @Override
    public boolean anthology$isTimeDilatedHurt() {
        return this.anthology$timeDilatedHurt;
    }

    @Override
    public void anthology$setTimeDilatedHurt(boolean value) {
        this.anthology$timeDilatedHurt = value;
    }

    @Override
    public List<Afterimage> anthology$getAfterimages() {
        return this.anthology$afterimages;
    }

    @Override
    public void anthology$setAfterimages(List<Afterimage> afterimages) {
        this.anthology$afterimages = afterimages;
    }
}
