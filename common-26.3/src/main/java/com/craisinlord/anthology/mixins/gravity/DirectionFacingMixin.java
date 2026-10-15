package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityApi;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.Comparator;

@Mixin(Direction.class)
public abstract class DirectionFacingMixin {
    @Inject(method = "orderedByNearest", at = @At("HEAD"), cancellable = true)
    private static void anthology$fixEntityFacingOrder(Entity entity, CallbackInfoReturnable<Direction[]> cir) {
        if (!GravityApi.isGravityInverted(entity)) {
            return;
        }
        Vec3 look = entity.getViewVector(1.0F);

        Direction[] sorted = Direction.values();
        Arrays.sort(sorted, Comparator.comparingDouble(
                (Direction d) -> d.getStepX() * look.x + d.getStepY() * look.y + d.getStepZ() * look.z
        ).reversed());

        cir.setReturnValue(sorted);
    }
}
