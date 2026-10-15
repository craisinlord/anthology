package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity")
@Pseudo
public abstract class IronsSpellbooksTargetedAreaTimeDilationMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lio/redspace/ironsspellbooks/entity/spells/target_area/TargetedAreaEntity;tickCount:I"))
    private int anthology$dilatedDuration(int tickCount) {
        return TimeDilationApi.getDilatedTickCount((Entity) (Object) this);
    }
}
