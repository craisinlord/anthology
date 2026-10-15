package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.TimeDilationApi;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Creeper.class)
public abstract class CreeperTimeDilationMixin {
    @ModifyReturnValue(method = "getSwellDir", at = @At("RETURN"))
    private int anthology$slowSwellProgress(int direction) {
        Creeper creeper = (Creeper) (Object) this;
        return TimeDilationApi.consumeTick(creeper, "creeper_swell") ? direction : 0;
    }
}
