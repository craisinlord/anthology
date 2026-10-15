package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.PathfinderMob;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "io.redspace.ironsspellbooks.entity.mobs.goals.WizardAttackGoal")
@Pseudo
public abstract class IronsSpellbooksWizardAttackGoalTimeDilationMixin {
    @Shadow
    @Final
    protected PathfinderMob mob;

    @WrapMethod(method = "tick")
    private void anthology$scaleAttackTimers(Operation<Void> original) {
        int ticks = TimeDilationApi.consumeTicks(this.mob, "irons_wizard_attack_goal");
        for (int i = 0; i < ticks && this.mob.isAlive(); i++) {
            original.call();
        }
    }
}
