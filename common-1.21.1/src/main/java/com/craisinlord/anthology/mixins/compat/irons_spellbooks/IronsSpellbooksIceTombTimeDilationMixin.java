package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "io.redspace.ironsspellbooks.entity.spells.ice_tomb.IceTombEntity")
@Pseudo
public abstract class IronsSpellbooksIceTombTimeDilationMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lio/redspace/ironsspellbooks/entity/spells/ice_tomb/IceTombEntity;tickCount:I", ordinal = 1))
    private int anthology$dilatedLifetime(int tickCount) {
        return TimeDilationApi.getDilatedTickCount((Entity) (Object) this);
    }
}
