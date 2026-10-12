package com.craisinlord.anthology.gravity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;

public final class GravityHooks {
    @FunctionalInterface
    public interface CameraRollSource {
        float rollDegrees(float partialTick);
    }

    private static final List<Predicate<Entity>> INVERTED_REGIONS = new CopyOnWriteArrayList<>();
    private static final List<Predicate<FluidState>> INVERTED_FLUIDS = new CopyOnWriteArrayList<>();
    private static final List<Consumer<Entity>> LOAD_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<CameraRollSource> CAMERA_ROLL_SOURCES = new CopyOnWriteArrayList<>();

    private GravityHooks() {
    }

    public static void addInvertedRegion(Predicate<Entity> region) {
        INVERTED_REGIONS.add(region);
    }

    public static void addInvertedFluid(Predicate<FluidState> fluid) {
        INVERTED_FLUIDS.add(fluid);
    }

    public static void addLoadListener(Consumer<Entity> listener) {
        LOAD_LISTENERS.add(listener);
    }

    public static void addCameraRollSource(CameraRollSource source) {
        CAMERA_ROLL_SOURCES.add(source);
    }

    public static boolean isInInvertedRegion(Entity entity) {
        for (Predicate<Entity> region : INVERTED_REGIONS) {
            if (region.test(entity)) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldInvertInRegion(Entity entity) {
        return GravityApi.isGravityInverted(entity) && isInInvertedRegion(entity);
    }

    public static boolean isInvertedFluid(FluidState fluidState) {
        if (fluidState.isEmpty()) {
            return false;
        }
        for (Predicate<FluidState> fluid : INVERTED_FLUIDS) {
            if (fluid.test(fluidState)) {
                return true;
            }
        }
        return false;
    }

    public static void onGravityLoaded(Entity entity) {
        for (Consumer<Entity> listener : LOAD_LISTENERS) {
            listener.accept(entity);
        }
    }

    public static float cameraRollDegrees(float partialTick) {
        float roll = 0.0F;
        for (CameraRollSource source : CAMERA_ROLL_SOURCES) {
            roll += source.rollDegrees(partialTick);
        }
        return roll;
    }
}
