package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.time.client.TimeDilationSpeedModifier;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "io.redspace.ironsspellbooks.render.animation.AnimationHelper")
@Pseudo
public abstract class IronsSpellbooksPlayerAnimationTimeDilationMixin {
    @ModifyReturnValue(method = "lambda$initializePlayerAnimationFactory$1(Lnet/minecraft/client/player/AbstractClientPlayer;)Ldev/kosmx/playerAnim/api/layered/IAnimation;", at = @At("RETURN"))
    private static IAnimation anthology$dilateSpellAnimations(IAnimation animation, @Local(argsOnly = true) AbstractClientPlayer player) {
        if (animation instanceof ModifierLayer<?> layer) {
            layer.addModifier(new TimeDilationSpeedModifier(player), 0);
        }
        return animation;
    }
}
