package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.effect.AnthologyEffects;
import com.craisinlord.anthology.time.client.ClientTimeDilationTicker;
import com.craisinlord.anthology.time.TimeDilationApi;
import com.craisinlord.anthology.time.TimeDilationMath;
import com.craisinlord.anthology.effect.ContractedMobEffect;
import com.craisinlord.anthology.effect.DilatedMobEffect;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(SoundEngine.class)
public abstract class SoundEngineTimeDilationMixin {
    @Shadow @Final private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Inject(method = "tickInGameSound", at = @At("TAIL"))
    private void anthology$refreshActiveSoundPitches(CallbackInfo ci) {
        if (!ClientTimeDilationTicker.hasActiveDilation()) {
            return;
        }
        this.instanceToChannel.forEach((sound, channel) ->
                channel.execute(audioChannel -> audioChannel.setPitch(this.anthology$dilatePitch(sound.getPitch(), sound))));
    }

    @ModifyReturnValue(method = "calculatePitch", at = @At("RETURN"))
    private float anthology$dilatePitch(float pitch, SoundInstance sound) {
        double rate = ClientTimeDilationTicker.rateAt(sound.getX(), sound.getY(), sound.getZ());
        if (sound instanceof EntityBoundSoundInstanceAccessor accessor) {
            double entityRate = rateForEntity(accessor.anthology$getEntity());
            rate = combineRates(rate, entityRate);
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            double listenerRate = rateForEntity(minecraft.player);
            rate = combineRates(rate, listenerRate);
        }
        if (Math.abs(rate - TimeDilationMath.NORMAL_RATE) < 0.001D) {
            return pitch;
        }
        return (float) (pitch * Math.max(TimeDilationMath.MIN_RATE, rate));
    }

    private static double combineRates(double first, double second) {
        if (first < TimeDilationMath.NORMAL_RATE || second < TimeDilationMath.NORMAL_RATE) {
            return Math.min(first, second);
        }
        return Math.max(first, second);
    }

    private static double rateForEntity(Entity entity) {
        double rate = TimeDilationApi.getRate(entity);
        if (entity instanceof LivingEntity living) {
            if (AnthologyEffects.dilated() != null && living.hasEffect(AnthologyEffects.dilated())) {
                rate = Math.min(rate, DilatedMobEffect.rateForAmplifier(
                        living.getEffect(AnthologyEffects.dilated()).getAmplifier()));
            }
            if (AnthologyEffects.contracted() != null && living.hasEffect(AnthologyEffects.contracted())) {
                rate = combineRates(rate, ContractedMobEffect.rateForAmplifier(
                        living.getEffect(AnthologyEffects.contracted()).getAmplifier()));
            }
        }
        return rate;
    }
}
