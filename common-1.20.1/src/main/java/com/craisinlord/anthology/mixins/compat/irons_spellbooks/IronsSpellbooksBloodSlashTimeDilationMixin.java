package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.craisinlord.anthology.time.TimeDilationMath;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(targets = "io.redspace.ironsspellbooks.entity.spells.blood_slash.BloodSlashProjectile")
@Pseudo
public abstract class IronsSpellbooksBloodSlashTimeDilationMixin {
    @Unique
    private double anthology$motionRate() {
        return Math.max(TimeDilationMath.MIN_RATE, TimeDilationApi.getRate((Entity) (Object) this));
    }

    @ModifyConstant(require = 0, method = {"tick", "m_8119_"}, constant = @Constant(floatValue = 0.12F))
    private float anthology$dilateGrowth(float growth) {
        return (float) (growth * this.anthology$motionRate());
    }
}
