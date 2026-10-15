package com.craisinlord.anthology.portal.client;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;

public final class PortalViewUniforms {
    private static final List<GpuBuffer> PROJECTIONS = new ArrayList<>();
    private static final List<GpuBuffer> FOGS = new ArrayList<>();
    private static final List<GlobalSettingsUniform> GLOBALS = new ArrayList<>();
    private static int projectionCursor;
    private static int fogCursor;
    private static int globalsCursor;

    private PortalViewUniforms() {
    }

    static void beginFrame() {
        projectionCursor = 0;
        fogCursor = 0;
        globalsCursor = 0;
    }

    public static GpuBufferSlice projection(Matrix4fc matrix) {
        GpuBuffer buffer = next(PROJECTIONS, projectionCursor++, RenderSystem.PROJECTION_MATRIX_UBO_SIZE, "projection");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE).putMat4f(matrix).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), data);
        }
        return buffer.slice(0L, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
    }

    static GpuBufferSlice fog(FogData fog) {
        GpuBuffer buffer = next(FOGS, fogCursor++, FogRenderer.FOG_UBO_SIZE, "fog");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, FogRenderer.FOG_UBO_SIZE)
                    .putVec4(fog.color)
                    .putFloat(fog.environmentalStart)
                    .putFloat(fog.environmentalEnd)
                    .putFloat(fog.renderDistanceStart)
                    .putFloat(fog.renderDistanceEnd)
                    .putFloat(fog.skyEnd)
                    .putFloat(fog.cloudEnd)
                    .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), data);
        }
        return buffer.slice(0L, FogRenderer.FOG_UBO_SIZE);
    }

    static void globals(int width, int height, double glintAlpha, long gameTime, DeltaTracker deltaTracker, int menuBlurRadius,
                        Vec3 cameraPos, boolean useRgss) {
        while (GLOBALS.size() <= globalsCursor) {
            GLOBALS.add(new GlobalSettingsUniform());
        }
        GLOBALS.get(globalsCursor++).update(width, height, glintAlpha, gameTime, deltaTracker.getGameTimeDeltaPartialTick(false), menuBlurRadius,
                cameraPos, useRgss);
    }

    static void clear() {
        for (GpuBuffer buffer : PROJECTIONS) {
            buffer.close();
        }
        for (GpuBuffer buffer : FOGS) {
            buffer.close();
        }
        for (GlobalSettingsUniform uniform : GLOBALS) {
            uniform.close();
        }
        PROJECTIONS.clear();
        FOGS.clear();
        GLOBALS.clear();
        beginFrame();
    }

    private static GpuBuffer next(List<GpuBuffer> pool, int index, int size, String name) {
        while (pool.size() <= index) {
            pool.add(RenderSystem.getDevice().createBuffer(() -> "Portal gun " + name + " UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, size));
        }
        return pool.get(index);
    }
}
