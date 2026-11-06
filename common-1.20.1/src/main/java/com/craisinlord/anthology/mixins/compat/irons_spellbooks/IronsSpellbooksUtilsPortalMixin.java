package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.compat.irons_spellbooks.IronsSpellbooksCompat;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Mixin(targets = "io.redspace.ironsspellbooks.api.util.Utils")
@Pseudo
public abstract class IronsSpellbooksUtilsPortalMixin {
    @WrapMethod(require = 0, method = "handleSpellTeleport", remap = false)
    private static boolean anthology$applyPortalLook(AbstractSpell spell, Entity entity, Vec3 destination, Operation<Boolean> original) {
        boolean canceled = original.call(spell, entity, destination);
        if (canceled) {
            IronsSpellbooksCompat.clearPortalLook(entity);
        } else {
            IronsSpellbooksCompat.applyPortalLook(entity, destination);
        }
        return canceled;
    }
}
