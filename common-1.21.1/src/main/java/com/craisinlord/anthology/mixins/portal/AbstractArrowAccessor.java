package com.craisinlord.anthology.mixins.portal;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
    @Accessor("inGround")
    void anthology$setInGround(boolean inGround);

    @Accessor("inGroundTime")
    void anthology$setInGroundTime(int inGroundTime);

    @Accessor("shakeTime")
    void anthology$setShakeTime(int shakeTime);
}
