package com.craisinlord.anthology.portal.client;

import com.craisinlord.anthology.portal.PortalEntity;
import com.craisinlord.anthology.mixins.portal.client.LevelRendererPortalSceneInvoker;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class PortalSceneRenderer {
    private static RenderBuffers portalBuffers;
    private static boolean renderingScene;

    private PortalSceneRenderer() {
    }

    public static MultiBufferSource.BufferSource bufferSource() {
        if (portalBuffers == null) {
            portalBuffers = new RenderBuffers();
        }
        return portalBuffers.bufferSource();
    }

    public static boolean isRenderingScene() {
        return renderingScene;
    }

    public static void render(Minecraft minecraft, Camera camera, float partialTick, Matrix4f viewMatrix,
                              Matrix4f projection, Matrix4f skyProjection, Frustum frustum,
                              PortalTerrain terrain, Consumer<PoseStack> crossingEntities,
                              UUID sourceId, UUID destinationId, int viewLevel) {
        LevelRenderer renderer = minecraft.levelRenderer;
        LevelRendererPortalSceneInvoker scene = (LevelRendererPortalSceneInvoker) renderer;
        Vec3 cameraPosition = camera.getPosition();
        double cameraX = cameraPosition.x;
        double cameraY = cameraPosition.y;
        double cameraZ = cameraPosition.z;

        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting previousSorting = RenderSystem.getVertexSorting();
        Matrix3f previousInverseViewRotation = new Matrix3f(RenderSystem.getInverseViewRotationMatrix());
        float previousFogStart = RenderSystem.getShaderFogStart();
        float previousFogEnd = RenderSystem.getShaderFogEnd();
        float[] previousFogColor = RenderSystem.getShaderFogColor().clone();
        FogShape previousFogShape = RenderSystem.getShaderFogShape();
        Camera previousBlockEntityCamera = minecraft.getBlockEntityRenderDispatcher().camera;
        modelViewStack.pushPose();
        modelViewStack.setIdentity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setInverseViewRotationMatrix(new Matrix3f(viewMatrix).invert());
        renderingScene = true;
        boolean irisPipelineInstalled = IrisPortalSceneCompat.begin(renderer);
        terrain.begin(minecraft);
        try {
            float renderDistance = minecraft.gameRenderer.getRenderDistance();
            BlockPos cameraBlockPos = BlockPos.containing(cameraX, cameraY, cameraZ);
            boolean foggy = minecraft.level.effects().isFoggyAt(cameraBlockPos.getX(), cameraBlockPos.getY())
                    || minecraft.gui.getBossOverlay().shouldCreateWorldFog();
            PoseStack poseStack = new PoseStack();
            poseStack.last().pose().set(viewMatrix);
            poseStack.last().normal().set(viewMatrix);

            {
                RenderSystem.setShaderGameTime(minecraft.level.getGameTime(), partialTick);
                minecraft.getBlockEntityRenderDispatcher().prepare(minecraft.level, camera, minecraft.hitResult);
                minecraft.getEntityRenderDispatcher().prepare(minecraft.level, camera, minecraft.crosshairPickEntity);
                FogRenderer.setupColor(camera, partialTick, minecraft.level, minecraft.options.getEffectiveRenderDistance(),
                        minecraft.gameRenderer.getDarkenWorldAmount(partialTick));
                FogRenderer.levelFogColor();
                fillBackground(RenderSystem.getShaderFogColor());
                RenderSystem.setProjectionMatrix(skyProjection, VertexSorting.DISTANCE_TO_ORIGIN);
                FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_SKY, renderDistance, foggy, partialTick);
                RenderSystem.setShader(GameRenderer::getPositionShader);
                if (irisPipelineInstalled || PortalSkyQueries.shouldRenderSky(sourceId, destinationId, viewLevel)) {
                    renderer.renderSky(poseStack, skyProjection, partialTick, camera, foggy,
                            () -> FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_SKY, renderDistance, foggy, partialTick));
                }
            }

            RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
            FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_TERRAIN, Math.max(renderDistance, 32.0F), foggy, partialTick);
            {
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
                terrain.drawLayer(minecraft, RenderType.solid(), cameraPosition, viewMatrix, projection);
                terrain.drawLayer(minecraft, RenderType.cutoutMipped(), cameraPosition, viewMatrix, projection);
                terrain.drawLayer(minecraft, RenderType.cutout(), cameraPosition, viewMatrix, projection);
            }

            if (minecraft.level.effects().constantAmbientLight()) {
                Lighting.setupNetherLevel(viewMatrix);
            } else {
                Lighting.setupLevel(viewMatrix);
            }
            MultiBufferSource.BufferSource buffers = bufferSource();
            try {
                {
                    for (Entity entity : minecraft.level.entitiesForRendering()) {
                        if (!minecraft.getEntityRenderDispatcher().shouldRender(entity, frustum, cameraX, cameraY, cameraZ)
                                && !entity.hasIndirectPassenger(minecraft.player)) {
                            continue;
                        }
                        if (entity instanceof PortalEntity portal && !portal.shouldRenderFront(cameraPosition)) {
                            continue;
                        }
                        if (entity == camera.getEntity()
                                && entity.getBoundingBox().inflate(0.1D).contains(cameraPosition)
                                && (!(entity instanceof LivingEntity living) || !living.isSleeping())) {
                            continue;
                        }
                        scene.anthology$renderPortalEntity(entity, cameraX, cameraY, cameraZ, partialTick, poseStack, buffers);
                    }
                    crossingEntities.accept(poseStack);
                }
                buffers.endBatch();

                {
                    terrain.forEachBlockEntity(minecraft, frustum, blockEntity ->
                            renderBlockEntity(minecraft, blockEntity, cameraX, cameraY, cameraZ, partialTick, poseStack, buffers));
                }
                buffers.endBatch();
                if (!irisPipelineInstalled) {
                    PortalSkyQueries.querySkyVisibility(sourceId, destinationId, viewLevel);
                }

                {
                    terrain.drawLayer(minecraft, RenderType.translucent(), cameraPosition, viewMatrix, projection);
                    buffers.endBatch();
                    terrain.drawLayer(minecraft, RenderType.tripwire(), cameraPosition, viewMatrix, projection);
                    minecraft.particleEngine.render(poseStack, buffers, minecraft.gameRenderer.lightTexture(), camera, partialTick);
                }

                modelViewStack.pushPose();
                modelViewStack.mulPoseMatrix(viewMatrix);
                RenderSystem.applyModelViewMatrix();
                try {
                    RenderSystem.depthMask(false);
                    scene.anthology$renderPortalWeather(minecraft.gameRenderer.lightTexture(), partialTick, cameraX, cameraY, cameraZ);
                    scene.anthology$renderPortalWorldBorder(camera);
                } finally {
                    modelViewStack.popPose();
                    RenderSystem.applyModelViewMatrix();
                }
            } finally {
                buffers.endBatch();
            }
        } finally {
            renderingScene = false;
            IrisPortalSceneCompat.end(renderer, irisPipelineInstalled);
            terrain.end(minecraft);
            modelViewStack.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(previousProjection, previousSorting);
            RenderSystem.setInverseViewRotationMatrix(previousInverseViewRotation);
            RenderSystem.setShaderFogStart(previousFogStart);
            RenderSystem.setShaderFogEnd(previousFogEnd);
            RenderSystem.setShaderFogColor(previousFogColor[0], previousFogColor[1], previousFogColor[2], previousFogColor[3]);
            RenderSystem.setShaderFogShape(previousFogShape);
            if (previousBlockEntityCamera != null) {
                minecraft.getBlockEntityRenderDispatcher().prepare(minecraft.level, previousBlockEntityCamera, minecraft.hitResult);
            }
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static void fillBackground(float[] fogColor) {
        int previousDepthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting previousSorting = RenderSystem.getVertexSorting();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);
        buffer.vertex(-1.0D, -1.0D, 0.99999D).endVertex();
        buffer.vertex(3.0D, -1.0D, 0.99999D).endVertex();
        buffer.vertex(-1.0D, 3.0D, 0.99999D).endVertex();
        BufferBuilder.RenderedBuffer mesh = buffer.end();
        try {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            GL11.glDepthFunc(GL11.GL_ALWAYS);
            RenderSystem.setProjectionMatrix(new Matrix4f(), previousSorting);
            RenderSystem.setShader(GameRenderer::getPositionShader);
            RenderSystem.setShaderColor(fogColor[0], fogColor[1], fogColor[2], 1.0F);
            BufferUploader.drawWithShader(mesh);
        } finally {
            GL11.glDepthFunc(previousDepthFunction);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setProjectionMatrix(previousProjection, previousSorting);
            RenderSystem.enableCull();
        }
    }

    private static void renderBlockEntity(Minecraft minecraft, BlockEntity blockEntity, double cameraX, double cameraY, double cameraZ,
                                          float partialTick, PoseStack poseStack, MultiBufferSource.BufferSource buffers) {
        if (!blockEntity.hasLevel() || !blockEntity.getType().isValid(blockEntity.getBlockState())) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        poseStack.pushPose();
        try {
            poseStack.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);
            minecraft.getBlockEntityRenderDispatcher().render(blockEntity, partialTick, poseStack, buffers);
        } finally {
            poseStack.popPose();
        }
    }
}
