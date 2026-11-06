package com.craisinlord.anthology.gravity.client;

import com.craisinlord.anthology.gravity.GravityApi;
import java.lang.ref.WeakReference;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class GravityCameraSmoothing {
    private static final float FLIP_SMOOTH_SECONDS = 0.35F;
    private static final float FLIP_MAX_SPEED = 0.6F;
    private static final float FLIP_SETTLE_DISTANCE = 0.002F;
    private static final double SHIFT_SMOOTH_SECONDS = 0.3D;
    private static final double SHIFT_SETTLE_DISTANCE = 0.001D;
    private static final double MAX_ABSORBED_SHIFT = 4.0D;
    private static final double VELOCITY_ALLOWANCE = 1.5D;
    private static final double MOVEMENT_MARGIN = 0.05D;
    private static final double TICKS_PER_SECOND = 20.0D;
    private static final float MAX_FRAME_SECONDS = 0.1F;
    private static final int SETTLE_TICKS = 20;

    private static WeakReference<Entity> tracked = new WeakReference<>(null);
    private static long lastNanos;
    private static float progress;
    private static float progressVelocity;
    private static double lastY;
    private static double shift;
    private static double shiftVelocity;

    private GravityCameraSmoothing() {
    }

    public static float flipProgress(Entity entity, float partialTick) {
        if (!GravityClientSettings.smoothTransitions()) {
            tracked.clear();
            shift = 0.0D;
            shiftVelocity = 0.0D;
            return GravityApi.getGravityFlipProgress(entity, partialTick);
        }
        advance(entity, partialTick);
        return progress;
    }

    public static double verticalShift() {
        return shift;
    }

    private static void advance(Entity entity, float partialTick) {
        long now = System.nanoTime();
        double y = entity.getPosition(partialTick).y;
        float target = GravityApi.isGravityInverted(entity) ? 1.0F : 0.0F;
        if (tracked.get() != entity || entity.tickCount < SETTLE_TICKS) {
            tracked = new WeakReference<>(entity);
            lastNanos = now;
            progress = target;
            progressVelocity = 0.0F;
            lastY = y;
            shift = 0.0D;
            shiftVelocity = 0.0D;
            return;
        }

        float seconds = Minecraft.getInstance().isPaused() ? 0.0F : Math.min((now - lastNanos) / 1.0E9F, MAX_FRAME_SECONDS);
        lastNanos = now;
        if (progress != target) {
            absorbPositionShift(entity, y - lastY, seconds);
        }
        lastY = y;
        if (seconds > 0.0F) {
            stepProgress(target, seconds);
            stepShift(seconds);
        }
    }

    private static void absorbPositionShift(Entity entity, double movedY, float seconds) {
        double allowedY = Math.abs(entity.getDeltaMovement().y) * seconds * TICKS_PER_SECOND * VELOCITY_ALLOWANCE + MOVEMENT_MARGIN;
        double excess = movedY - Math.max(-allowedY, Math.min(allowedY, movedY));
        if (Math.abs(excess) > MAX_ABSORBED_SHIFT) {
            shift = 0.0D;
            shiftVelocity = 0.0D;
            return;
        }
        shift -= excess;
    }

    private static void stepProgress(float target, float seconds) {
        float omega = 2.0F / FLIP_SMOOTH_SECONDS;
        float x = omega * seconds;
        float decay = 1.0F / (1.0F + x + 0.48F * x * x + 0.235F * x * x * x);
        float maxChange = FLIP_MAX_SPEED * FLIP_SMOOTH_SECONDS;
        float change = Math.max(-maxChange, Math.min(maxChange, progress - target));
        float clampedTarget = progress - change;
        float temp = (progressVelocity + omega * change) * seconds;
        progressVelocity = (progressVelocity - omega * temp) * decay;
        float next = clampedTarget + (change + temp) * decay;
        boolean passedTarget = (target > progress) == (next > target);
        if (passedTarget || (Math.abs(next - target) < FLIP_SETTLE_DISTANCE && Math.abs(progressVelocity) < FLIP_SETTLE_DISTANCE)) {
            progress = target;
            progressVelocity = 0.0F;
            return;
        }
        progress = next;
    }

    private static void stepShift(float seconds) {
        if (shift == 0.0D) {
            return;
        }
        double omega = 2.0D / SHIFT_SMOOTH_SECONDS;
        double x = omega * seconds;
        double decay = 1.0D / (1.0D + x + 0.48D * x * x + 0.235D * x * x * x);
        double temp = (shiftVelocity + omega * shift) * seconds;
        shiftVelocity = (shiftVelocity - omega * temp) * decay;
        shift = (shift + temp) * decay;
        if (Math.abs(shift) < SHIFT_SETTLE_DISTANCE && Math.abs(shiftVelocity) < SHIFT_SETTLE_DISTANCE) {
            shift = 0.0D;
            shiftVelocity = 0.0D;
        }
    }
}
