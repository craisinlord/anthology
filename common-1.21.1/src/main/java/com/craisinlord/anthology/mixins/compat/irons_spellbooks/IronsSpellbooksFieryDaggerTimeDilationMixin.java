package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "io.redspace.ironsspellbooks.entity.spells.fiery_dagger.FieryDaggerEntity")
@Pseudo
public abstract class IronsSpellbooksFieryDaggerTimeDilationMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lio/redspace/ironsspellbooks/entity/spells/fiery_dagger/FieryDaggerEntity;age:I", ordinal = 0))
    private int anthology$dilatedHoverDelay(int age) {
        return age + TimeDilationApi.consumeTicks((Entity) (Object) this, "irons_fiery_dagger_delay") - 1;
    }
}
