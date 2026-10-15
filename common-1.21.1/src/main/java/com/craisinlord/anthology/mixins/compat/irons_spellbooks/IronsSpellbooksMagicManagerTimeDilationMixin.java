package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Mixin(targets = "io.redspace.ironsspellbooks.capabilities.magic.MagicManager")
@Pseudo
public abstract class IronsSpellbooksMagicManagerTimeDilationMixin {
    @WrapMethod(method = "lambda$tick$0(ZLnet/minecraft/world/entity/player/Player;)V")
    private void anthology$scalePlayerMagicTick(boolean doManaRegen, Player player, Operation<Void> original) {
        int ticks = TimeDilationApi.consumeTicks(player, "irons_magic_tick");
        for (int i = 0; i < ticks && !player.isRemoved(); i++) {
            original.call(doManaRegen, player);
        }
    }
}
