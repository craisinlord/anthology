package com.craisinlord.anthology.portal.client;

import com.craisinlord.anthology.portal.PortalEntity;
import com.craisinlord.anthology.portal.PortalRegistry;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class PortalRim {
    public static final float EXTRA_WIDTH = 0.0F;
    public static final float EXTRA_HEIGHT = 0.0F;
    public static final int ENTITY_ALPHA = 150;
    private static final double OVERLAY_OFFSET = 0.05D;
    private static final int HIDDEN_RIM_ALPHA = 170;

    private PortalRim() {
    }

    public static Identifier texture(PortalEntity portal) {
        return portal.appearance().rimTexture();
    }

    static void drawOverView(RenderTarget target, PortalEntity portal, Vec3 cameraPos, Matrix4f viewMatrix, Matrix4f projectionMatrix,
                             float partialTick) {
        drawRims(target, PortalViewPipelines.RIM_OVERLAY, List.of(portal), cameraPos, viewMatrix,
                PortalViewUniforms.projection(projectionMatrix), partialTick, ENTITY_ALPHA);
    }

    static void drawHiddenOutlines(Minecraft minecraft, RenderTarget target, Vec3 cameraPos, Matrix4f viewMatrix, Matrix4f projectionMatrix,
                                   float partialTick) {
        drawHiddenOutlines(minecraft, target, cameraPos, viewMatrix, projectionMatrix, partialTick, null);
    }

    static void drawHiddenOutlines(Minecraft minecraft, RenderTarget target, Vec3 cameraPos, Matrix4f viewMatrix, Matrix4f projectionMatrix,
                                   float partialTick, PortalEntity excluded) {
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        List<PortalEntity> ownedPortals = collectOwnedPortals(minecraft);
        if (excluded != null) {
            ownedPortals.remove(excluded);
        }
        if (ownedPortals.isEmpty()) {
            return;
        }
        drawRims(target, PortalViewPipelines.RIM_HIDDEN, ownedPortals, cameraPos, viewMatrix,
                PortalViewUniforms.projection(projectionMatrix), partialTick, HIDDEN_RIM_ALPHA);
    }

    private static List<PortalEntity> collectOwnedPortals(Minecraft minecraft) {
        PortalClientHooks.OutlinePolicy policy = PortalClientHooks.outlinePolicy();
        List<PortalEntity> portals = new ArrayList<>();
        for (PortalEntity portal : PortalRegistry.all(minecraft.level)) {
            if (portal.isAlive() && policy.shouldOutline(portal, minecraft.player)) {
                portals.add(portal);
            }
        }
        return portals;
    }

    private static void drawRims(RenderTarget target, RenderPipeline pipeline, List<PortalEntity> portals, Vec3 cameraPos,
                                 Matrix4f viewMatrix, GpuBufferSlice projection, float partialTick, int alpha) {
        Map<Identifier, List<PortalEntity>> byTexture = new LinkedHashMap<>();
        for (PortalEntity portal : portals) {
            byTexture.computeIfAbsent(texture(portal), ignored -> new ArrayList<>()).add(portal);
        }
        for (Map.Entry<Identifier, List<PortalEntity>> entry : byTexture.entrySet()) {
            drawRimsWithTexture(target, pipeline, entry.getValue(), cameraPos, viewMatrix, projection, partialTick, alpha, entry.getKey());
        }
    }

    private static void drawRimsWithTexture(RenderTarget target, RenderPipeline pipeline, List<PortalEntity> portals, Vec3 cameraPos,
                                            Matrix4f viewMatrix, GpuBufferSlice projection, float partialTick, int alpha,
                                            Identifier texture) {
        BufferBuilder buffer = PortalViewDraw.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (PortalEntity portal : portals) {
            addRim(buffer, portal, cameraPos, partialTick, alpha);
        }
        MeshData mesh = buffer.build();
        if (mesh == null) {
            return;
        }
        AbstractTexture rimTexture = Minecraft.getInstance().getTextureManager().getTexture(texture);
        PortalViewDraw.draw(target, pipeline, mesh, viewMatrix, projection, rimTexture.getTextureView(), rimTexture.getSampler());
    }

    private static void addRim(BufferBuilder buffer, PortalEntity portal, Vec3 cameraPos, float partialTick, int alpha) {
        float scale = portal.getPortalVisualScale(partialTick);
        if (scale <= 0.0F) {
            return;
        }
        Vec3 right = portal.getWidthVec().normalize();
        Vec3 up = portal.getUpVec().normalize();
        Vec3 center = portal.position().add(portal.getNormalVec().normalize().scale(OVERLAY_OFFSET)).subtract(cameraPos);
        double halfWidth = (portal.getPortalWidth() * 0.5D + EXTRA_WIDTH) * scale;
        double halfHeight = (portal.getPortalHeight() * 0.5D + EXTRA_HEIGHT) * scale;
        int color = portal.appearance().color();
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        addVertex(buffer, center, right, up, -halfWidth, -halfHeight, 0.0F, 1.0F, red, green, blue, alpha);
        addVertex(buffer, center, right, up, -halfWidth, halfHeight, 0.0F, 0.0F, red, green, blue, alpha);
        addVertex(buffer, center, right, up, halfWidth, halfHeight, 1.0F, 0.0F, red, green, blue, alpha);
        addVertex(buffer, center, right, up, halfWidth, -halfHeight, 1.0F, 1.0F, red, green, blue, alpha);
    }

    private static void addVertex(BufferBuilder buffer, Vec3 center, Vec3 right, Vec3 up, double horizontal, double vertical,
                                  float u, float v, int red, int green, int blue, int alpha) {
        Vec3 position = center.add(right.scale(horizontal)).add(up.scale(vertical));
        buffer.addVertex((float) position.x, (float) position.y, (float) position.z)
                .setUv(u, v)
                .setColor(red, green, blue, alpha);
    }
}
