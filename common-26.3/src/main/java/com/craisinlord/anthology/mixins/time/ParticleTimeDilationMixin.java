package com.craisinlord.anthology.mixins.time;

import com.craisinlord.anthology.time.client.ClientTimeDilationTicker;
import com.craisinlord.anthology.time.TimeDilationMath;
import net.minecraft.client.particle.Particle;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

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

    @ModifyArgs(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;move(DDD)V")
    )
    private void anthology$slowParticleMovement(Args args) {
        double rate = this.anthology$sampleRate();
        if (rate >= TimeDilationMath.NORMAL_RATE) {
            return;
        }
        args.set(0, (Double) args.get(0) * rate);
        args.set(1, (Double) args.get(1) * rate);
        args.set(2, (Double) args.get(2) * rate);
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
