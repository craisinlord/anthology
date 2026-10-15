package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.client.ClientTimeDilationTicker;
import com.craisinlord.anthology.time.TimeDilationMath;
import net.minecraft.client.particle.Particle;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Particle.class)
public abstract class ParticleTimeDilationMixin {
    @Shadow
    protected int age;
    @Shadow
    protected double x;
    @Shadow
    protected double y;
    @Shadow
    protected double z;

    @Unique
    private double anthology$ageProgress;

    @Unique
    private double anthology$sampleRate() {
        return ClientTimeDilationTicker.rateAt(this.x, this.y, this.z);
    }

    @ModifyArg(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;move(DDD)V"),
            index = 0
    )
    private double anthology$slowParticleMovementX(double value) {
        return this.anthology$scaleMovement(value);
    }

    @ModifyArg(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;move(DDD)V"),
            index = 1
    )
    private double anthology$slowParticleMovementY(double value) {
        return this.anthology$scaleMovement(value);
    }

    @ModifyArg(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;move(DDD)V"),
            index = 2
    )
    private double anthology$slowParticleMovementZ(double value) {
        return this.anthology$scaleMovement(value);
    }

    @Unique
    private double anthology$scaleMovement(double value) {
        double rate = this.anthology$sampleRate();
        return rate >= TimeDilationMath.NORMAL_RATE ? value : value * rate;
    }

    @Redirect(
            method = "tick",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/Particle;age:I", opcode = Opcodes.PUTFIELD)
    )
    private void anthology$slowParticleAge(Particle self, int nextAge) {
        double rate = this.anthology$sampleRate();
        if (rate >= TimeDilationMath.NORMAL_RATE) {
            this.age = nextAge;
            return;
        }
        this.anthology$ageProgress += rate;
        if (this.anthology$ageProgress >= 1.0D) {
            this.anthology$ageProgress -= 1.0D;
            this.age = nextAge;
        }
    }
}
