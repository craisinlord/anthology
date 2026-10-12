package com.craisinlord.anthology.mixins.gravity;

import com.craisinlord.anthology.gravity.GravityHooks;
import com.craisinlord.anthology.gravity.GravityAccess;
import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityDirection;
import com.craisinlord.anthology.gravity.GravityRotationUtil;
import com.craisinlord.anthology.gravity.GravityTransition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGravityMixin implements GravityAccess {
    @Unique
    private static final String TAG_PREFIX = "Anthology";
    @Unique
    private static final String LEGACY_TAG_PREFIX = "Antarchy";
    @Unique
    private static final String GRAVITY_DIRECTION_TAG = "GravityDirection";
    @Unique
    private static final String PREV_GRAVITY_DIRECTION_TAG = "PrevGravityDirection";
    @Unique
    private static final String GRAVITY_FORCED_TAG = "GravityForced";
    @Unique
    private static final String GRAVITY_TRANSITION_DURATION_TAG = "GravityTransitionDuration";
    @Unique
    private static final String GRAVITY_TRANSITION_REMAINING_TAG = "GravityTransitionRemaining";

    @Unique
    private GravityDirection anthology$gravityDirection = GravityDirection.DOWN;
    @Unique
    private GravityDirection anthology$prevGravityDirection = GravityDirection.DOWN;
    @Unique
    private boolean anthology$gravityForced;
    @Unique
    private int anthology$gravityTransitionDuration;
    @Unique
    private int anthology$gravityTransitionRemaining;

    @Shadow
    protected abstract AABB makeBoundingBox();

    @Shadow
    public abstract void setBoundingBox(AABB boundingBox);

    @Inject(method = "saveWithoutId", at = @At("TAIL"))
    private void anthology$saveGravityState(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        tag.putString(TAG_PREFIX + GRAVITY_DIRECTION_TAG, this.anthology$gravityDirection.getSerializedName());
        tag.putString(TAG_PREFIX + PREV_GRAVITY_DIRECTION_TAG, this.anthology$prevGravityDirection.getSerializedName());
        tag.putBoolean(TAG_PREFIX + GRAVITY_FORCED_TAG, this.anthology$gravityForced);
        tag.putInt(TAG_PREFIX + GRAVITY_TRANSITION_DURATION_TAG, this.anthology$gravityTransitionDuration);
        tag.putInt(TAG_PREFIX + GRAVITY_TRANSITION_REMAINING_TAG, this.anthology$gravityTransitionRemaining);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void anthology$readGravityState(CompoundTag tag, CallbackInfo ci) {
        this.anthology$gravityDirection = GravityDirection.parse(tag.getString(anthology$tagKey(tag, GRAVITY_DIRECTION_TAG)))
                .orElse(GravityDirection.DOWN);
        this.anthology$prevGravityDirection = GravityDirection.parse(tag.getString(anthology$tagKey(tag, PREV_GRAVITY_DIRECTION_TAG)))
                .orElse(this.anthology$gravityDirection);
        this.anthology$gravityForced = tag.getBoolean(anthology$tagKey(tag, GRAVITY_FORCED_TAG));
        this.anthology$gravityTransitionDuration = Math.max(0, tag.getInt(anthology$tagKey(tag, GRAVITY_TRANSITION_DURATION_TAG)));
        this.anthology$gravityTransitionRemaining = Math.max(0, tag.getInt(anthology$tagKey(tag, GRAVITY_TRANSITION_REMAINING_TAG)));
        GravityHooks.onGravityLoaded(this.anthology$entity());
    }

    @Unique
    private static String anthology$tagKey(CompoundTag tag, String name) {
        String key = TAG_PREFIX + name;
        return tag.contains(key) ? key : LEGACY_TAG_PREFIX + name;
    }

    @Inject(method = "baseTick", at = @At("TAIL"))
    private void anthology$tickGravityTransition(CallbackInfo ci) {
        if (this.anthology$gravityTransitionRemaining > 0) {
            this.anthology$gravityTransitionRemaining--;
        }
    }

    @Inject(method = "updateInWaterStateAndDoFluidPushing", at = @At("TAIL"))
    private void anthology$resetFallDistanceInAntiwater(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = this.anthology$entity();
        GravityDirection direction = this.anthology$gravityDirection;
        if (!direction.isInverted()) {
            return;
        }

        if (!anthology$isTouchingAntiwater(entity)) {
            return;
        }

        entity.resetFallDistance();
    }

    @Override
    public GravityDirection anthology$getGravityDirection() {
        return this.anthology$gravityDirection;
    }

    @Override
    public GravityDirection anthology$getPrevGravityDirection() {
        return this.anthology$prevGravityDirection;
    }

    @Override
    public boolean anthology$isGravityForced() {
        return this.anthology$gravityForced;
    }

    @Override
    public int anthology$getGravityTransitionDuration() {
        return this.anthology$gravityTransitionDuration;
    }

    @Override
    public int anthology$getGravityTransitionRemaining() {
        return this.anthology$gravityTransitionRemaining;
    }

    @Override
    public void anthology$setGravityState(GravityDirection direction, boolean forced, GravityTransition transition) {
        GravityDirection previousDirection = this.anthology$gravityDirection;
        boolean changed = this.anthology$gravityDirection != direction || this.anthology$gravityForced != forced;
        if (this.anthology$gravityDirection != direction) {
            this.anthology$adjustPositionForGravityChange(this.anthology$gravityDirection, direction);
            this.anthology$prevGravityDirection = this.anthology$gravityDirection;
            this.anthology$gravityDirection = direction;
            this.anthology$gravityTransitionDuration = transition.durationTicks();
            this.anthology$gravityTransitionRemaining = transition.durationTicks();
            this.anthology$refreshGravityBounds();
        }

        this.anthology$gravityForced = forced;
        if (changed && this.anthology$entity() instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        if (changed && !this.anthology$entity().level().isClientSide) {
            GravityApi.notifyGravityStateChanged(this.anthology$entity());
        }
    }

    @Override
    public void anthology$setAirborneGravityState(GravityDirection direction, boolean forced, GravityTransition transition) {
        Entity entity = this.anthology$entity();
        net.minecraft.world.phys.Vec3 worldVelocity = GravityApi.getWorldVelocity(entity);
        this.anthology$setGravityState(direction, forced, transition);
        GravityApi.setWorldVelocity(entity, worldVelocity);
        entity.setOnGround(false);
        entity.resetFallDistance();
    }

    @Override
    public void anthology$applySyncedGravityState(
            GravityDirection direction,
            GravityDirection previousDirection,
            boolean forced,
            int transitionDuration,
            int transitionRemaining
    ) {
        GravityDirection oldDirection = this.anthology$gravityDirection;
        this.anthology$gravityDirection = direction;
        this.anthology$prevGravityDirection = previousDirection;
        this.anthology$gravityForced = forced;
        this.anthology$gravityTransitionDuration = Math.max(0, transitionDuration);
        this.anthology$gravityTransitionRemaining = Math.max(0, transitionRemaining);
        if (oldDirection != direction) {
            this.anthology$refreshGravityBounds();
        }
    }

    @Unique
    private Entity anthology$entity() {
        return (Entity) (Object) this;
    }

    @Unique
    private void anthology$refreshGravityBounds() {
        this.setBoundingBox(this.makeBoundingBox());
    }

    @Override
    public void anthology$resetGravityState() {
        this.anthology$gravityDirection = GravityDirection.DOWN;
        this.anthology$prevGravityDirection = GravityDirection.DOWN;
        this.anthology$gravityForced = false;
        this.anthology$gravityTransitionDuration = 0;
        this.anthology$gravityTransitionRemaining = 0;
        this.anthology$refreshGravityBounds();
    }

    @Unique
    private void anthology$adjustPositionForGravityChange(GravityDirection oldDirection, GravityDirection newDirection) {
        if (oldDirection == newDirection) {
            return;
        }

        Entity entity = this.anthology$entity();
        double yOffset = newDirection.isInverted() ? entity.getBbHeight() : -entity.getBbHeight();
        entity.setPos(entity.getX(), entity.getY() + yOffset, entity.getZ());
        entity.setOnGround(false);
        entity.resetFallDistance();
    }

    @ModifyVariable(method = "checkFallDamage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double anthology$invertFallDamageY(double worldY) {
        Entity entity = (Entity) (Object) this;
        if (!GravityApi.isGravityInverted(entity) || entity instanceof Player) {
            return worldY;
        }

        return -worldY;
    }

    @Unique
    private boolean anthology$isTouchingAntiwater(Entity entity) {
        return anthology$intersectsAntiwater(entity, entity.getBoundingBox().inflate(0.05D));
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

    @ModifyVariable(method = "isSupportedBy", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private BlockPos anthology$mirrorSupportBlock(BlockPos pos) {
        Entity entity = (Entity) (Object) this;
        if (!GravityApi.isGravityInverted(entity)) {
            return pos;
        }

        return pos.above();
    }
}
