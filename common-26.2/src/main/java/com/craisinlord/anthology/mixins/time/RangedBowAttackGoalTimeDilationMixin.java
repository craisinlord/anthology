package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.TimeDilationApi;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RangedBowAttackGoal.class)
public abstract class RangedBowAttackGoalTimeDilationMixin {
    @Unique
    private Mob anthology$timeDilationMob;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/monster/Monster;DIF)V", at = @At("RETURN"))
    private void anthology$captureMob(Monster mob, double speedModifier, int attackIntervalMin, float attackRadius, CallbackInfo ci) {
        this.anthology$timeDilationMob = mob;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void anthology$slowRangedBowGoalTick(CallbackInfo ci) {
        if (this.anthology$timeDilationMob != null && !TimeDilationApi.consumeTick(this.anthology$timeDilationMob, "ranged_bow_goal_tick")) {
            ci.cancel();
        }
    }
}
