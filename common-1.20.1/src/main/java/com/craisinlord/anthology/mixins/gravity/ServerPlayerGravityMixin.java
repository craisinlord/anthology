package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityHooks;
import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityDirection;
import com.craisinlord.anthology.gravity.GravityRotationUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerGravityMixin {
    @Unique
    private float anthology$invertedFallDistance = 0.0F;

    @Inject(method = "doCheckFallDamage", at = @At("HEAD"), cancellable = true)
    private void anthology$cancelFallDamageInAntiwater(double x, double y, double z, boolean onGround, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        GravityDirection gravity = GravityApi.getGravityDirection(self);
        if (!gravity.isInverted()) {
            return;
        }
        if (!anthology$isTouchingAntiwater(self)) {
            return;
        }

        self.fallDistance = 0.0F;
        this.anthology$invertedFallDistance = 0.0F;
        ci.cancel();
    }

    @WrapOperation(method = "doCheckFallDamage",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;checkFallDamage(DZLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V"))
    private void anthology$fixFallDamage(ServerPlayer instance,
                                         double worldY, boolean onGround,
                                         BlockState state, BlockPos pos,
                                         Operation<Void> original,
                                         @Local(ordinal = 0) double dx,
                                         @Local(ordinal = 1) double dy,
                                         @Local(ordinal = 2) double dz) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        GravityDirection gravity = GravityApi.getGravityDirection(self);
        if (!gravity.isInverted()) {
            this.anthology$invertedFallDistance = 0.0F;
            original.call(instance, worldY, onGround, state, pos);
            return;
        }
        if (anthology$isTouchingAntiwater(self) || anthology$passedThroughAntiwater(self, dx, dy, dz)) {
            self.fallDistance = 0.0F;
            this.anthology$invertedFallDistance = 0.0F;
            original.call(instance, 0.0D, onGround, state, pos);
            return;
        }
        Vec3 localVec = GravityRotationUtil.vecWorldToPlayer(new Vec3(dx, dy, dz), gravity);
        if (!onGround) {
            if (localVec.y < 0.0) {
                this.anthology$invertedFallDistance += (float)(-localVec.y);
            } else {
                this.anthology$invertedFallDistance = 0.0F;
            }
        } else if (this.anthology$invertedFallDistance > self.fallDistance) {
            self.fallDistance = this.anthology$invertedFallDistance;
        }
        original.call(instance, localVec.y, onGround, state, pos);
        if (onGround) {
            this.anthology$invertedFallDistance = 0.0F;
        }
    }

    @Unique
    private boolean anthology$isTouchingAntiwater(ServerPlayer player) {
        return anthology$intersectsAntiwater(player, player.getBoundingBox().inflate(0.05D));
    }

    @Unique
    private boolean anthology$passedThroughAntiwater(ServerPlayer player, double dx, double dy, double dz) {
        AABB sweptBox = player.getBoundingBox().expandTowards(-dx, -dy, -dz).inflate(0.05D);
        return anthology$intersectsAntiwater(player, sweptBox);
    }

    @Unique
    private boolean anthology$intersectsAntiwater(ServerPlayer player, AABB box) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    cursor.set(x, y, z);
                    FluidState fluidState = player.level().getFluidState(cursor);
                    if (GravityHooks.isInvertedFluid(fluidState)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
