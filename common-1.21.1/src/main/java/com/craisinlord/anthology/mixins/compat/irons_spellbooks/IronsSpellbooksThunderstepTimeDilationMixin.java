package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "io.redspace.ironsspellbooks.entity.spells.thunderstep.ThunderstepProjectile")
@Pseudo
public abstract class IronsSpellbooksThunderstepTimeDilationMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lio/redspace/ironsspellbooks/entity/spells/thunderstep/ThunderstepProjectile;tickCount:I"))
    private int anthology$dilatedGroundTime(int tickCount) {
        return TimeDilationApi.getDilatedTickCount((Entity) (Object) this);
    }
}
