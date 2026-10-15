package com.craisinlord.anthology.portal.client;

import com.craisinlord.anthology.portal.PortalEntity;
import com.craisinlord.anthology.mixins.portal.client.GameRendererPortalAccessor;
import com.craisinlord.anthology.mixins.portal.client.LevelRendererPortalViewAreaAccessor;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.WorldBorderRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.client.renderer.state.level.WorldBorderRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

public final class PortalSceneRenderer {
    private static final float SKY_FLASH_THRESHOLD = 1.0E-5F;
    private static WeatherEffectRenderer weatherRenderer;
    private static WorldBorderRenderer worldBorderRenderer;
    private static boolean renderingScene;

    private PortalSceneRenderer() {
    }

    public static boolean isRenderingScene() {
        return renderingScene;
    }

    public static void render(Minecraft minecraft, RenderTarget target, Camera camera, CameraRenderState mainCameraState,
                              DeltaTracker deltaTracker, float partialTick, Matrix4f viewMatrix, Matrix4f projection, Matrix4f skyProjection,
                              Frustum frustum, PortalTerrain terrain, Consumer<List<EntityRenderState>> crossingEntities,
                              UUID sourceId, UUID destinationId, int viewLevel, int[] clearRegion, Supplier<MeshData> farAperture) {
        LevelRenderer renderer = minecraft.levelRenderer;
        Vec3 cameraPosition = camera.position();
        double cameraX = cameraPosition.x;
        double cameraY = cameraPosition.y;
        double cameraZ = cameraPosition.z;
        OptionsRenderState options = minecraft.gameRenderer.getGameRenderState().optionsRenderState;
        int renderDistance = minecraft.options.getEffectiveRenderDistance();

        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        GpuBufferSlice previousProjection = RenderSystem.getProjectionMatrixBuffer();
        ProjectionType previousProjectionType = RenderSystem.getProjectionType();
        GpuBufferSlice previousFog = RenderSystem.getShaderFog();
        GpuBuffer previousGlobals = RenderSystem.getGlobalSettingsUniform();
        modelViewStack.pushMatrix();
        modelViewStack.set(viewMatrix);
        renderingScene = true;
        boolean irisPipelineInstalled = IrisPortalSceneCompat.begin(renderer);
        FeatureRenderDispatcher features = minecraft.gameRenderer.getFeatureRenderDispatcher();
        SubmitNodeStorage submits = features.getSubmitNodeStorage();
        try {
            FogData fog = ((GameRendererPortalAccessor) minecraft.gameRenderer).anthology$getFogRenderer().setupFog(camera, renderDistance,
                    deltaTracker, minecraft.gameRenderer.getBossOverlayWorldDarkening(partialTick), minecraft.level);
            RenderSystem.setShaderFog(PortalViewUniforms.fog(fog));
            PortalViewUniforms.globals(target.width, target.height, options.glintStrength, minecraft.level.getGameTime(), deltaTracker,
                    options.menuBackgroundBlurriness, cameraPosition, options.textureFiltering == TextureFilteringMethod.RGSS);
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(),
                    ARGB.colorFromFloat(0.0F, fog.color.x, fog.color.y, fog.color.z), target.getDepthTexture(), 1.0D,
                    clearRegion[0], clearRegion[1], clearRegion[2], clearRegion[3]);

            if (!minecraft.gui.getBossOverlay().shouldCreateWorldFog()
                    && (irisPipelineInstalled || PortalSkyQueries.shouldRenderSky(sourceId, destinationId, viewLevel))) {
                RenderSystem.setProjectionMatrix(PortalViewUniforms.projection(skyProjection), ProjectionType.PERSPECTIVE);
                renderSky(minecraft, camera, partialTick);
            }

            RenderSystem.setProjectionMatrix(PortalViewUniforms.projection(projection), ProjectionType.PERSPECTIVE);
            terrain.begin(minecraft, viewMatrix, projection);
            terrain.drawGroup(minecraft, ChunkSectionLayerGroup.OPAQUE);
            minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.LEVEL);

            CameraRenderState cameraState = cameraState(camera, mainCameraState, viewMatrix, projection, frustum);
            minecraft.getEntityRenderDispatcher().prepare(camera, minecraft.crosshairPickEntity);
            minecraft.getBlockEntityRenderDispatcher().prepare(cameraPosition);
            List<EntityRenderState> entityStates = new ArrayList<>();
            for (Entity entity : minecraft.level.entitiesForRendering()) {
                if (!minecraft.getEntityRenderDispatcher().shouldRender(entity, frustum, cameraX, cameraY, cameraZ)
                        && !entity.hasIndirectPassenger(minecraft.player)) {
                    continue;
                }
                if (entity instanceof PortalEntity portal && !portal.shouldRenderFront(cameraPosition)) {
                    continue;
                }
                if (entity == camera.entity()
                        && entity.getBoundingBox().inflate(0.1D).contains(cameraPosition)
                        && (!(entity instanceof LivingEntity living) || !living.isSleeping())) {
                    continue;
                }
                float entityPartialTick = deltaTracker.getGameTimeDeltaPartialTick(!minecraft.level.tickRateManager().isEntityFrozen(entity));
                entityStates.add(minecraft.getEntityRenderDispatcher().extractEntity(entity, entityPartialTick));
            }
            crossingEntities.accept(entityStates);
            List<BlockEntityRenderState> blockEntityStates = new ArrayList<>();
            terrain.forEachBlockEntity(minecraft, frustum, blockEntity -> {
                BlockEntityRenderState state = extractBlockEntity(minecraft, blockEntity, partialTick);
                if (state != null) {
                    blockEntityStates.add(state);
                }
            });
            ParticlesRenderState particles = new ParticlesRenderState();
            minecraft.particleEngine.extract(particles, frustum, camera, partialTick);

            PoseStack poseStack = new PoseStack();
            for (EntityRenderState state : entityStates) {
                state.outlineColor = 0;
                minecraft.getEntityRenderDispatcher().submit(state, cameraState, state.x - cameraX, state.y - cameraY, state.z - cameraZ,
                        poseStack, submits);
            }
            for (BlockEntityRenderState state : blockEntityStates) {
                BlockPos pos = state.blockPos;
                poseStack.pushPose();
                poseStack.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);
                minecraft.getBlockEntityRenderDispatcher().submit(state, poseStack, submits, cameraState);
                poseStack.popPose();
            }
            particles.submit(submits, cameraState);

            features.renderSolidFeatures();
            minecraft.renderBuffers().bufferSource().endBatch();
            if (!irisPipelineInstalled) {
                PortalSkyQueries.querySkyVisibility(sourceId, destinationId, viewLevel, target, farAperture.get());
            }
            features.renderTranslucentFeatures();
            minecraft.renderBuffers().bufferSource().endBatch();
            minecraft.renderBuffers().crumblingBufferSource().endBatch();
            terrain.drawGroup(minecraft, ChunkSectionLayerGroup.TRANSLUCENT);
            features.renderTranslucentParticles();
            minecraft.renderBuffers().bufferSource().endBatch();
            particles.reset();

            renderWeatherAndBorder(minecraft, renderer, cameraPosition, partialTick, renderDistance, mainCameraState.depthFar);
        } finally {
            features.clearSubmitNodes();
            renderingScene = false;
            IrisPortalSceneCompat.end(renderer, irisPipelineInstalled);
            terrain.end(minecraft);
            modelViewStack.popMatrix();
            if (previousProjection != null) {
                RenderSystem.setProjectionMatrix(previousProjection, previousProjectionType);
            }
            if (previousFog != null) {
                RenderSystem.setShaderFog(previousFog);
            }
            if (previousGlobals != null) {
                RenderSystem.setGlobalSettingsUniform(previousGlobals);
            }
        }
    }

    private static CameraRenderState cameraState(Camera camera, CameraRenderState mainCameraState, Matrix4f viewMatrix, Matrix4f projection,
                                                 Frustum frustum) {
        CameraRenderState state = new CameraRenderState();
        state.initialized = true;
        state.pos = camera.position();
        state.blockPos = camera.blockPosition().immutable();
        state.xRot = camera.xRot();
        state.yRot = camera.yRot();
        state.orientation.set(camera.rotation());
        state.cullFrustum.set(frustum);
        state.depthFar = mainCameraState.depthFar;
        state.fogType = camera.getFluidInCamera();
        state.projectionMatrix.set(projection);
        state.viewRotationMatrix.set(viewMatrix);
        state.entityRenderState = mainCameraState.entityRenderState;
        state.hudFov = mainCameraState.hudFov;
        return state;
    }

    private static BlockEntityRenderState extractBlockEntity(Minecraft minecraft, BlockEntity blockEntity, float partialTick) {
        if (!blockEntity.hasLevel() || !blockEntity.getType().isValid(blockEntity.getBlockState())) {
            return null;
        }
        return minecraft.getBlockEntityRenderDispatcher().tryExtractRenderState(blockEntity, partialTick, null);
    }

    private static void renderSky(Minecraft minecraft, Camera camera, float partialTick) {
        SkyRenderer skyRenderer = ((LevelRendererPortalViewAreaAccessor) minecraft.levelRenderer).anthology$getSkyRenderer();
        if (skyRenderer == null) {
            return;
        }
        FogType fogType = camera.getFluidInCamera();
        if (fogType == FogType.POWDER_SNOW || fogType == FogType.LAVA) {
            return;
        }
        if (camera.entity() instanceof LivingEntity living && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) {
            return;
        }
        SkyRenderState state = new SkyRenderState();
        skyRenderer.extractRenderState(minecraft.level, partialTick, camera, state);
        if (state.skybox == DimensionType.Skybox.NONE) {
            return;
        }
        if (state.skybox == DimensionType.Skybox.END) {
            skyRenderer.renderEndSky();
            if (state.endFlashIntensity > SKY_FLASH_THRESHOLD) {
                skyRenderer.renderEndFlash(new PoseStack(), state.endFlashIntensity, state.endFlashXAngle, state.endFlashYAngle);
            }
            return;
        }
        PoseStack poseStack = new PoseStack();
        skyRenderer.renderSkyDisc(state.skyColor);
        skyRenderer.renderSunriseAndSunset(poseStack, state.sunAngle, state.sunriseAndSunsetColor);
        skyRenderer.renderSunMoonAndStars(poseStack, state.sunAngle, state.moonAngle, state.starAngle, state.moonPhase,
                state.rainBrightness, state.starBrightness);
        if (state.shouldRenderDarkDisc) {
            skyRenderer.renderDarkDisc();
        }
    }

    private static void renderWeatherAndBorder(Minecraft minecraft, LevelRenderer renderer, Vec3 cameraPosition, float partialTick,
                                               int renderDistance, float depthFar) {
        if (weatherRenderer == null) {
            weatherRenderer = new WeatherEffectRenderer();
        }
        if (worldBorderRenderer == null) {
            worldBorderRenderer = new WorldBorderRenderer();
        }
        WeatherRenderState weather = new WeatherRenderState();
        weatherRenderer.extractRenderState(minecraft.level, ((LevelRendererPortalViewAreaAccessor) renderer).anthology$getTicks(), partialTick, cameraPosition, weather);
        weatherRenderer.render(cameraPosition, weather);
        double borderDistance = renderDistance * 16.0D;
        WorldBorderRenderState border = new WorldBorderRenderState();
        worldBorderRenderer.extract(minecraft.level.getWorldBorder(), partialTick, cameraPosition, borderDistance, border);
        worldBorderRenderer.render(border, cameraPosition, borderDistance, depthFar);
    }
}
