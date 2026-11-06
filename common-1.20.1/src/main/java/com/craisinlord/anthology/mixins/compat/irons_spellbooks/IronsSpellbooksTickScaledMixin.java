package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.time.TimeDilationTickScaled;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Mixin(targets = {
        "io.redspace.ironsspellbooks.entity.spells.AoeEntity",
        "io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile",
        "io.redspace.ironsspellbooks.entity.spells.black_hole.BlackHole",
        "io.redspace.ironsspellbooks.entity.spells.ArrowVolleyEntity",
        "io.redspace.ironsspellbooks.entity.spells.ChainLightning",
        "io.redspace.ironsspellbooks.entity.spells.StompAoe",
        "io.redspace.ironsspellbooks.entity.spells.ExtendedEvokerFang",
        "io.redspace.ironsspellbooks.entity.spells.eldritch_blast.EldritchBlastVisualEntity",
        "io.redspace.ironsspellbooks.entity.spells.ray_of_frost.RayOfFrostVisualEntity"
})
@Pseudo
public abstract class IronsSpellbooksTickScaledMixin implements TimeDilationTickScaled {
}
