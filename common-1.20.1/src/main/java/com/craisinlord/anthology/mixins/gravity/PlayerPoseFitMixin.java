package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityApi;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class PlayerPoseFitMixin {
    @Inject(method = "canEnterPose", at = @At("HEAD"), cancellable = true)
    private void anthology$canFitPoseWhenInverted(Pose pose, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof Player player) || !GravityApi.isGravityInverted(player)) {
            return;
        }

        EntityDimensions dimensions = player.getDimensions(pose);
        Vec3 position = player.position();
        float halfWidth = dimensions.width / 2.0F;
        float height = dimensions.height;
        AABB invertedBox = new AABB(
                position.x - halfWidth, position.y - height, position.z - halfWidth,
                position.x + halfWidth, position.y,           position.z + halfWidth
        );
        cir.setReturnValue(player.level().noCollision(player, invertedBox));
    }
}
