package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "io.redspace.ironsspellbooks.entity.spells.ice_block.IceBlockProjectile")
@Pseudo
public abstract class IronsSpellbooksIceBlockTimeDilationMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lio/redspace/ironsspellbooks/entity/spells/ice_block/IceBlockProjectile;airTime:I", ordinal = 0))
    private int anthology$dilatedAirTime(int airTime) {
        return airTime - TimeDilationApi.consumeTicks((Entity) (Object) this, "irons_ice_block_air_time") + 1;
    }
}
