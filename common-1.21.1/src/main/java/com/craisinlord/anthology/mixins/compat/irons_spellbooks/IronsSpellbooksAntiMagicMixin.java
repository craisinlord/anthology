package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.portal.PortalEntity;
import com.craisinlord.anthology.time.TimeDilationFieldEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({TimeDilationFieldEntity.class, PortalEntity.class})
public abstract class IronsSpellbooksAntiMagicMixin implements AntiMagicSusceptible {
    @Override
    public void onAntiMagic(MagicData magicData) {
        Object self = this;
        if (self instanceof TimeDilationFieldEntity field && field.canBeDispelled()) {
            field.dispel();
        } else if (self instanceof PortalEntity portal && portal.canBeDispelled()) {
            portal.dispel();
        }
    }
}
