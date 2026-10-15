package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerRecasts;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "io.redspace.ironsspellbooks.player.ClientPlayerEvents")
@Pseudo
public abstract class IronsSpellbooksClientPlayerEventsTimeDilationMixin {
    @Unique
    private static int anthology$consumeTicks(String timerKey) {
        Player player = Minecraft.getInstance().player;
        return player == null ? 1 : TimeDilationApi.consumeTicks(player, timerKey);
    }

    @WrapOperation(method = "onPlayerTick", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/capabilities/magic/PlayerRecasts;tickRecasts()V"))
    private static void anthology$scaleRecasts(PlayerRecasts recasts, Operation<Void> original) {
        int ticks = anthology$consumeTicks("irons_client_recasts");
        for (int i = 0; i < ticks; i++) {
            original.call(recasts);
        }
    }

    @ModifyArg(method = "onPlayerTick", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/capabilities/magic/PlayerCooldowns;tick(I)V"))
    private static int anthology$scaleCooldowns(int ticks) {
        return ticks * anthology$consumeTicks("irons_client_cooldowns");
    }

    @WrapOperation(method = "onPlayerTick", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/player/ClientMagicData;handleCastDuration()V"))
    private static void anthology$scaleCastDuration(Operation<Void> original) {
        int ticks = anthology$consumeTicks("irons_client_cast");
        for (int i = 0; i < ticks; i++) {
            original.call();
        }
    }
}
