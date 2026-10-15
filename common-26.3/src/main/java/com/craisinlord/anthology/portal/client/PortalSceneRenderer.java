package com.craisinlord.anthology.portal.client;

import com.craisinlord.anthology.portal.PortalEntity;
import com.craisinlord.anthology.mixins.portal.client.GameRendererPortalAccessor;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;

public final class PortalSceneRenderer {
    private static WeatherEffectRenderer weatherRenderer;
    private static WorldBorderRenderer worldBorderRenderer;
    private static final Map<RenderTarget, SkyRenderer> SKY_RENDERERS = new IdentityHashMap<>();
    private static SkyRenderer vanillaSkyRenderer;
    private static boolean renderingScene;

    private PortalSceneRenderer() {
    }

    public static boolean isRenderingScene() {
        return renderingScene;
    }

    static void releaseSkyRenderers() {
        for (SkyRenderer skyRenderer : SKY_RENDERERS.values()) {
            skyRenderer.close();
        }
        SKY_RENDERERS.clear();
        vanillaSkyRenderer = null;
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
        OptionsRenderState options = minecraft.gameRenderer.gameRenderState().optionsRenderState;
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
        FeatureRenderDispatcher features = minecraft.gameRenderer.featureRenderDispatcher();
        SubmitNodeStorage submits = new SubmitNodeStorage();
        FeatureRenderDispatcher.PreparedFrame featureFrame = null;
        try {
            FogData fog = ((GameRendererPortalAccessor) minecraft.gameRenderer).anthology$getFogRenderer().setupFog(camera, renderDistance,
                    deltaTracker, minecraft.gameRenderer.bossOverlayWorldDarkening(partialTick), minecraft.level);
            GpuBufferSlice fogBuffer = PortalViewUniforms.fog(fog);
            RenderSystem.setShaderFog(fogBuffer);
            PortalViewUniforms.globals(target.width, target.height, options.glintStrength, minecraft.level.getGameTime(), deltaTracker,
                    options.menuBackgroundBlurriness, cameraPosition, options.textureFiltering == TextureFilteringMethod.RGSS);
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(),
                    new Vector4f(fog.color.x, fog.color.y, fog.color.z, 0.0F), target.getDepthTexture(), 0.0D,
                    clearRegion[0], clearRegion[1], clearRegion[2], clearRegion[3], 0);

            if (!minecraft.gui.hud.getBossOverlay().shouldCreateWorldFog()
                    && (irisPipelineInstalled || PortalSkyQueries.shouldRenderSky(sourceId, destinationId, viewLevel))) {
                RenderSystem.setProjectionMatrix(PortalViewUniforms.projection(skyProjection), ProjectionType.PERSPECTIVE);
                renderSky(minecraft, target, camera, partialTick, fogBuffer);
                RenderSystem.setShaderFog(fogBuffer);
            }

            RenderSystem.setProjectionMatrix(PortalViewUniforms.projection(projection), ProjectionType.PERSPECTIVE);
            terrain.begin(minecraft, viewMatrix, projection);
            minecraft.gameRenderer.lighting().setupFor(Lighting.Entry.LEVEL);

            CameraRenderState cameraState = cameraState(camera, mainCameraState, viewMatrix, projection, frustum);
            minecraft.getEntityRenderDispatcher().prepare(camera, minecraft.crosshairPickEntity);
            minecraft.getBlockEntityRenderDispatcher().prepare(cameraPosition);
            List<EntityRenderState> entityStates = new ArrayList<>();
            for (Entity entity : minecraft.level.entitiesForRendering()) {
                float entityPartialTick = deltaTracker.getGameTimeDeltaPartialTick(!minecraft.level.tickRateManager().isEntityFrozen(entity));
                if (!minecraft.getEntityRenderDispatcher().shouldRender(entity, frustum, cameraX, cameraY, cameraZ, entityPartialTick)
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
                entityStates.add(minecraft.getEntityRenderDispatcher().extractEntity(entity, entityPartialTick));
            }
            crossingEntities.accept(entityStates);
            List<BlockEntityRenderState> blockEntityStates = new ArrayList<>();
            terrain.forEachBlockEntity(minecraft, frustum, (blockEntity, globallyRendered) -> {
                BlockEntityRenderState state = extractBlockEntity(minecraft, blockEntity, partialTick, globallyRendered);
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

            featureFrame = features.prepareFrame(submits);
            try (RenderPass pass = openScenePass(target, "Portal gun portal view solid")) {
                terrain.drawGroup(minecraft, ChunkSectionLayerGroup.OPAQUE, pass);
                featureFrame.executeSolid(pass);
            }
            if (!irisPipelineInstalled) {
                PortalSkyQueries.querySkyVisibility(sourceId, destinationId, viewLevel, target, farAperture.get());
            }
            WeatherRenderState weather = prepareWeather(minecraft, cameraPosition, partialTick);
            double borderDistance = renderDistance * 16.0D;
            WorldBorderRenderState border = prepareBorder(minecraft, cameraPosition, partialTick, borderDistance, mainCameraState.depthFar);
            RenderSystem.resizeAllAutoStorageIndexBuffers();
            try (RenderPass pass = openScenePass(target, "Portal gun portal view translucent")) {
                featureFrame.executeTranslucent(pass);
                terrain.drawGroup(minecraft, ChunkSectionLayerGroup.TRANSLUCENT, pass);
                featureFrame.executeTranslucentAfterTerrain(pass);
                weatherRenderer.render(weather, pass);
                worldBorderRenderer.render(border, pass, cameraPosition, borderDistance);
            }
            particles.reset();
        } finally {
            if (featureFrame != null) {
                featureFrame.close();
            }
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
        state.smartCull = mainCameraState.smartCull;
        state.cameraEntityPartialTicks = mainCameraState.cameraEntityPartialTicks;
        state.fogData = mainCameraState.fogData;
        state.fogType = camera.getFluidInCamera();
        state.projectionMatrix.set(projection);
        state.viewRotationMatrix.set(viewMatrix);
        state.entityRenderState = mainCameraState.entityRenderState;
        state.hudFov = mainCameraState.hudFov;
        return state;
    }

    private static BlockEntityRenderState extractBlockEntity(Minecraft minecraft, BlockEntity blockEntity, float partialTick,
                                                             boolean globallyRendered) {
        if (!blockEntity.hasLevel() || !blockEntity.getType().isValid(blockEntity.getBlockState())) {
            return null;
        }
        return minecraft.getBlockEntityRenderDispatcher().tryExtractRenderState(blockEntity, partialTick, null, globallyRendered);
    }

    private static SkyRenderer skyRenderer(Minecraft minecraft, RenderTarget target) {
        SkyRenderer vanilla = minecraft.levelRenderer.skyRenderer();
        if (vanilla == null) {
            return null;
        }
        if (vanilla != vanillaSkyRenderer) {
            releaseSkyRenderers();
            vanillaSkyRenderer = vanilla;
        }
        return SKY_RENDERERS.computeIfAbsent(target,
                key -> new SkyRenderer(minecraft.getTextureManager(), minecraft.getAtlasManager(), key));
    }

    private static void renderSky(Minecraft minecraft, RenderTarget target, Camera camera, float partialTick, GpuBufferSlice fog) {
        SkyRenderer skyRenderer = skyRenderer(minecraft, target);
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
        skyRenderer.render(fog, state);
    }

    private static RenderPass openScenePass(RenderTarget target, String label) {
        RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> label, target.getColorTextureView(),
                Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty());
        RenderSystem.bindDefaultUniforms(pass);
        return pass;
    }

    private static WeatherRenderState prepareWeather(Minecraft minecraft, Vec3 cameraPosition, float partialTick) {
        if (weatherRenderer == null) {
            weatherRenderer = new WeatherEffectRenderer();
        }
        WeatherRenderState weather = new WeatherRenderState();
        weatherRenderer.extractRenderState(minecraft.level, partialTick, cameraPosition, weather);
        weatherRenderer.prepare(cameraPosition, weather);
        return weather;
    }

    private static WorldBorderRenderState prepareBorder(Minecraft minecraft, Vec3 cameraPosition, float partialTick, double borderDistance,
                                                        float depthFar) {
        if (worldBorderRenderer == null) {
            worldBorderRenderer = new WorldBorderRenderer();
        }
        WorldBorderRenderState border = new WorldBorderRenderState();
        worldBorderRenderer.extract(minecraft.level.getWorldBorder(), partialTick, cameraPosition, borderDistance, border);
        worldBorderRenderer.prepare(border, cameraPosition, borderDistance, depthFar);
        return border;
    }
}
