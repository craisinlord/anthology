package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Mixin(targets = "io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob")
@Pseudo
public abstract class IronsSpellbooksSpellCastingMobTimeDilationMixin {
    @WrapMethod(method = "customServerAiStep")
    private void anthology$scaleSpellCasting(Operation<Void> original) {
        Entity self = (Entity) (Object) this;
        int ticks = TimeDilationApi.consumeTicks(self, "irons_mob_casting");
        for (int i = 0; i < ticks && !self.isRemoved(); i++) {
            original.call();
        }
    }
}
