package com.craisinlord.anthology.portal.client;

import com.craisinlord.anthology.portal.PortalEntity;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.craisinlord.anthology.portal.PortalRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class PortalRim {
    public static final float EXTRA_WIDTH = 0.0F;
    public static final float EXTRA_HEIGHT = 0.0F;
    public static final int ENTITY_ALPHA = 150;
    private static final double OVERLAY_OFFSET = 0.05D;
    private static final int HIDDEN_RIM_ALPHA = 170;

    private PortalRim() {
    }

    public static ResourceLocation texture(PortalEntity portal) {
        return portal.appearance().rimTexture();
    }

    static void drawOverView(PortalEntity portal, Vec3 cameraPos, Matrix4f viewMatrix, Matrix4f projectionMatrix, float partialTick) {
        withCameraMatrices(viewMatrix, projectionMatrix, () -> {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.defaultBlendFunc();
            drawRims(List.of(portal), cameraPos, partialTick, ENTITY_ALPHA);
        });
    }

    static void drawHiddenOutlines(Minecraft minecraft, Vec3 cameraPos, Matrix4f viewMatrix, Matrix4f projectionMatrix, float partialTick) {
        drawHiddenOutlines(minecraft, cameraPos, viewMatrix, projectionMatrix, partialTick, null);
    }

    static void drawHiddenOutlines(Minecraft minecraft, Vec3 cameraPos, Matrix4f viewMatrix, Matrix4f projectionMatrix, float partialTick,
                                   PortalEntity excluded) {
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
        withCameraMatrices(viewMatrix, projectionMatrix, () -> {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.depthFunc(GL11.GL_GREATER);
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            drawRims(ownedPortals, cameraPos, partialTick, HIDDEN_RIM_ALPHA);
        });
    }

    private static void withCameraMatrices(Matrix4f viewMatrix, Matrix4f projectionMatrix, Runnable draw) {
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting previousSorting = RenderSystem.getVertexSorting();
        modelViewStack.pushPose();
        try {
            modelViewStack.setIdentity();
            modelViewStack.last().pose().set(viewMatrix);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(new Matrix4f(projectionMatrix), VertexSorting.DISTANCE_TO_ORIGIN);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            draw.run();
        } finally {
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.setProjectionMatrix(previousProjection, previousSorting);
            modelViewStack.popPose();
            RenderSystem.applyModelViewMatrix();
        }
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

    private static void drawRims(List<PortalEntity> portals, Vec3 cameraPos, float partialTick, int alpha) {
        Map<ResourceLocation, List<PortalEntity>> byTexture = new LinkedHashMap<>();
        for (PortalEntity portal : portals) {
            byTexture.computeIfAbsent(texture(portal), ignored -> new ArrayList<>()).add(portal);
        }
        for (Map.Entry<ResourceLocation, List<PortalEntity>> entry : byTexture.entrySet()) {
            drawRimsWithTexture(entry.getValue(), cameraPos, partialTick, alpha, entry.getKey());
        }
    }

    private static void drawRimsWithTexture(List<PortalEntity> portals, Vec3 cameraPos, float partialTick, int alpha, ResourceLocation texture) {
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (PortalEntity portal : portals) {
            addRim(buffer, portal, cameraPos, partialTick, alpha);
        }
        BufferBuilder.RenderedBuffer mesh = buffer.end();
        RenderSystem.setShaderTexture(0, texture);
        BufferUploader.drawWithShader(mesh);
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
        buffer.vertex(position.x, position.y, position.z)
                .uv(u, v)
                .color(red, green, blue, alpha)
                .endVertex();
    }
}
