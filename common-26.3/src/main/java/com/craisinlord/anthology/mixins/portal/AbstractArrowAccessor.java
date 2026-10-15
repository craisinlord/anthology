package com.craisinlord.anthology.mixins.portal;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
    @Invoker("setInGround")
    void anthology$setInGround(boolean inGround);

    @Accessor("inGroundTime")
    void anthology$setInGroundTime(int inGroundTime);

    @Accessor("shakeTime")
    void anthology$setShakeTime(int shakeTime);
}
