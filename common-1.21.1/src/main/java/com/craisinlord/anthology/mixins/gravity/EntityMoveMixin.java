package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityHooks;
import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityDirection;
import com.craisinlord.anthology.gravity.GravityRotationUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMoveMixin {
    @Unique
    private static final Logger STEP_LOGGER = LoggerFactory.getLogger("Anthology/GravityStep");
    @Unique
    private boolean anthology$loggedInvertedCollide;
    @Unique
    private boolean anthology$loggedHorizontalBlock;
    @Unique
    private boolean anthology$loggedInvertedMove;
    @Unique
    private double anthology$lastWorldMoveY = 0.0;
    @Unique
    private double anthology$lastPlayerMoveY = 0.0;
    @Unique
    private AABB anthology$moveStartBox;
    @Unique
    private Vec3 anthology$collideWorldMovement = Vec3.ZERO;

    @Inject(method = "move", at = @At("HEAD"))
    private void anthology$captureMoveStart(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        this.anthology$moveStartBox = entity.getBoundingBox();
        this.anthology$lastPlayerMoveY = movement.y;
        if (GravityApi.isGravityInverted(entity) && !this.anthology$loggedInvertedMove) {
            this.anthology$loggedInvertedMove = true;
            STEP_LOGGER.warn(
                    "Inverted move entered: entity={} movement={} noPhysics={} onGround={} box={}",
                    entity.getClass().getSimpleName(), movement, entity.noPhysics, entity.onGround(), entity.getBoundingBox());
        }
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private Vec3 anthology$movePlayerToWorld(Vec3 movement) {
        Entity entity = (Entity) (Object) this;
        GravityDirection direction = GravityApi.getGravityDirection(entity);
        if (!direction.isInverted()) {
            return movement;
        }

        Vec3 world = GravityRotationUtil.vecPlayerToWorld(movement, direction);
        this.anthology$lastWorldMoveY = world.y;
        return world;
    }

    @ModifyArg(
            method = "move",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;multiply(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0
            ),
            index = 0
    )
    private Vec3 anthology$moveMaskPlayerToWorld(Vec3 multiplier) {
        Entity entity = (Entity) (Object) this;
        GravityDirection direction = GravityApi.getGravityDirection(entity);
        if (!direction.isInverted()) {
            return multiplier;
        }

        return GravityRotationUtil.maskPlayerToWorld(multiplier, direction);
    }

    @ModifyVariable(
            method = "move",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V",
                    ordinal = 0
            ),
            ordinal = 0
    )
    private Vec3 anthology$moveWorldToPlayerAttempted(Vec3 movement) {
        Entity entity = (Entity) (Object) this;
        GravityDirection direction = GravityApi.getGravityDirection(entity);
        if (!direction.isInverted()) {
            return movement;
        }

        return GravityRotationUtil.vecWorldToPlayer(movement, direction);
    }

    @ModifyVariable(
            method = "move",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V",
                    ordinal = 0
            ),
            ordinal = 1
    )
    private Vec3 anthology$moveWorldToPlayerResolved(Vec3 movement) {
        Entity entity = (Entity) (Object) this;
        GravityDirection direction = GravityApi.getGravityDirection(entity);
        if (!direction.isInverted()) {
            return movement;
        }

        return GravityRotationUtil.vecWorldToPlayer(movement, direction);
    }

    @Inject(method = "move", at = @At("TAIL"))
    private void anthology$fixVerticalCollisionBelow(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (!GravityApi.isGravityInverted(entity)) {
            return;
        }

        if (this.anthology$lastPlayerMoveY < 0.0D && this.anthology$moveStartBox != null) {
            AABB endBox = entity.getBoundingBox();
            Vec3 delta = endBox.getCenter().subtract(this.anthology$moveStartBox.getCenter());
            AABB sweptBox = this.anthology$moveStartBox.expandTowards(delta).minmax(endBox).inflate(0.05D);
            if (anthology$intersectsAntiwater(entity, sweptBox)) {
                entity.resetFallDistance();
            }
        }

        if (!entity.verticalCollision) return;
        boolean landedOnCeiling = this.anthology$lastWorldMoveY > 0.0;
        entity.verticalCollisionBelow = landedOnCeiling;
        if (landedOnCeiling) {
            entity.setOnGround(true);
        }
    }

    @Inject(method = "collide", at = @At("HEAD"))
    private void anthology$captureCollideWorldMovement(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        this.anthology$collideWorldMovement = movement;
        Entity entity = (Entity) (Object) this;
        if (GravityApi.isGravityInverted(entity) && !this.anthology$loggedInvertedCollide) {
            this.anthology$loggedInvertedCollide = true;
            STEP_LOGGER.warn(
                    "Inverted collide entered: entity={} movement={} onGround={} maxUpStep={} box={}",
                    entity.getClass().getSimpleName(), movement, entity.onGround(), entity.maxUpStep(), entity.getBoundingBox());
        }
    }

    @ModifyVariable(
            method = "collide",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
                    ordinal = 0
            ),
            ordinal = 0
    )
    private Vec3 anthology$collideWorldToPlayer(Vec3 movement) {
        Entity entity = (Entity) (Object) this;
        GravityDirection direction = GravityApi.getGravityDirection(entity);
        if (!direction.isInverted()) {
            return movement;
        }
        return GravityRotationUtil.vecWorldToPlayer(movement, direction);
    }

    @ModifyArgs(
            method = "collide",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;expandTowards(DDD)Lnet/minecraft/world/phys/AABB;"
            )
    )
    private void anthology$expandStepSearchInWorldFrame(Args args) {
        Entity entity = (Entity) (Object) this;
        GravityDirection direction = GravityApi.getGravityDirection(entity);
        if (!direction.isInverted()) {
            return;
        }
        Vec3 world = GravityRotationUtil.vecPlayerToWorld(
                new Vec3(args.get(0), args.get(1), args.get(2)), direction);
        args.set(0, world.x);
        args.set(1, world.y);
        args.set(2, world.z);
    }

    @ModifyArgs(
            method = "collide",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;move(DDD)Lnet/minecraft/world/phys/AABB;"
            )
    )
    private void anthology$moveStepBoxInWorldFrame(Args args) {
        Entity entity = (Entity) (Object) this;
        GravityDirection direction = GravityApi.getGravityDirection(entity);
        if (!direction.isInverted()) {
            return;
        }
        Vec3 world = GravityRotationUtil.vecPlayerToWorld(
                new Vec3(args.get(0), args.get(1), args.get(2)), direction);
        args.set(0, world.x);
        args.set(1, world.y);
        args.set(2, world.z);
    }

    @WrapOperation(
            method = "collide",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private Vec3 anthology$collideBoundingBoxInLocalFrame(
            Entity collisionEntity,
            Vec3 localMovement,
            AABB box,
            Level level,
            List<VoxelShape> collisions,
            Operation<Vec3> original
    ) {
        GravityDirection direction = GravityApi.getGravityDirection(collisionEntity);
        if (!direction.isInverted()) {
            return original.call(collisionEntity, localMovement, box, level, collisions);
        }
        Vec3 worldMovement = GravityRotationUtil.vecPlayerToWorld(localMovement, direction);
        Vec3 worldResult = original.call(collisionEntity, worldMovement, box, level, collisions);
        return GravityRotationUtil.vecWorldToPlayer(worldResult, direction);
    }

    @WrapOperation(
            method = "collide",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;collideWithShapes(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private Vec3 anthology$collideStepCandidateInLocalFrame(
            Vec3 localMovement,
            AABB box,
            List<VoxelShape> collisions,
            Operation<Vec3> original
    ) {
        Entity entity = (Entity) (Object) this;
        GravityDirection direction = GravityApi.getGravityDirection(entity);
        if (!direction.isInverted()) {
            return original.call(localMovement, box, collisions);
        }
        Vec3 worldMovement = GravityRotationUtil.vecPlayerToWorld(localMovement, direction);
        Vec3 worldResult = original.call(worldMovement, box, collisions);
        return GravityRotationUtil.vecWorldToPlayer(worldResult, direction);
    }

    @Redirect(
            method = "collide",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;collectCandidateStepUpHeights(Lnet/minecraft/world/phys/AABB;Ljava/util/List;FF)[F"
            )
    )
    private float[] anthology$collectGravityAwareStepHeights(
            AABB box, List<VoxelShape> shapes, float maxStep, float currentY) {
        Entity entity = (Entity) (Object) this;
        boolean inverted = GravityApi.isGravityInverted(entity);
        TreeSet<Float> heights = new TreeSet<>();
        for (VoxelShape shape : shapes) {
            for (double coord : shape.getCoords(Direction.Axis.Y)) {
                float height = (float) (inverted ? box.maxY - coord : coord - box.minY);
                if (height >= 0.0F && height != currentY && height <= maxStep) {
                    heights.add(height);
                }
            }
        }
        float[] result = new float[heights.size()];
        int i = 0;
        for (float height : heights) {
            result[i++] = height;
        }
        return result;
    }

    @Redirect(
            method = "collide",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/phys/AABB;minY:D")
    )
    private double anthology$readLocalStepBase(AABB box) {
        Entity entity = (Entity) (Object) this;
        if (!GravityApi.isGravityInverted(entity)) {
            return box.minY;
        }
        return -box.maxY;
    }

    @Inject(method = "collide", at = @At("RETURN"), cancellable = true)
    private void anthology$collidePlayerToWorld(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        Entity entity = (Entity) (Object) this;
        GravityDirection direction = GravityApi.getGravityDirection(entity);
        if (!direction.isInverted()) {
            return;
        }
        Vec3 worldResult = GravityRotationUtil.vecPlayerToWorld(cir.getReturnValue(), direction);
        Vec3 localMovement = GravityRotationUtil.vecWorldToPlayer(this.anthology$collideWorldMovement, direction);
        if (!this.anthology$loggedHorizontalBlock
                && (localMovement.x != cir.getReturnValue().x || localMovement.z != cir.getReturnValue().z)) {
            this.anthology$loggedHorizontalBlock = true;
            STEP_LOGGER.warn(
                    "Inverted horizontal block: localMovement={} localResult={} worldMovement={} worldResult={} onGround={} maxUpStep={} box={}",
                    localMovement, cir.getReturnValue(), this.anthology$collideWorldMovement, worldResult,
                    entity.onGround(), entity.maxUpStep(), entity.getBoundingBox());
        }

        cir.setReturnValue(worldResult);
    }

    @Unique
    private boolean anthology$intersectsAntiwater(Entity entity, AABB box) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    cursor.set(x, y, z);
                    FluidState fluidState = entity.level().getFluidState(cursor);
                    if (GravityHooks.isInvertedFluid(fluidState)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
