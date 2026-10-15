package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.compat.irons_spellbooks.IronsSpellbooksCompat;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Mixin(targets = "io.redspace.ironsspellbooks.spells.ender.CounterspellSpell")
@Pseudo
public abstract class IronsSpellbooksCounterspellMixin {
    @WrapMethod(method = "onCast")
    private void anthology$dispelAnthologyMagic(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData magicData, Operation<Void> original) {
        Vec3 start = entity.getEyePosition();
        IronsSpellbooksCompat.enterPortalOpaque();
        try {
            original.call(level, spellLevel, entity, castSource, magicData);
        } finally {
            IronsSpellbooksCompat.exitPortalOpaque();
        }
        IronsSpellbooksCompat.RayPath path = IronsSpellbooksCompat.consumeRay();
        if (!level.isClientSide && path != null) {
            IronsSpellbooksCompat.dispelAlongRay(level, entity, start, path.end());
        }
    }
}
