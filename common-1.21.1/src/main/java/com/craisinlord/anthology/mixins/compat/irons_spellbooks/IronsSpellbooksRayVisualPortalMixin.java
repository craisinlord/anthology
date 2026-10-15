package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.compat.irons_spellbooks.IronsSpellbooksCompat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = {
        "io.redspace.ironsspellbooks.spells.ice.RayOfFrostSpell",
        "io.redspace.ironsspellbooks.spells.eldritch.EldritchBlastSpell"
})
@Pseudo
public abstract class IronsSpellbooksRayVisualPortalMixin {
    @WrapOperation(method = "onCast", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean anthology$splitVisualAtPortals(Level level, Entity visual, Operation<Boolean> original, @Local(argsOnly = true) LivingEntity caster) {
        List<Entity> segments = IronsSpellbooksCompat.splitRayVisual(level, visual, caster, IronsSpellbooksCompat.consumeRay());
        if (segments == null) {
            return original.call(level, visual);
        }
        boolean added = false;
        for (Entity segment : segments) {
            added |= original.call(level, segment);
        }
        return added;
    }
}
