package com.craisinlord.anthology.portal.client;

import com.craisinlord.anthology.Anthology;
import com.craisinlord.anthology.portal.PortalHooks;
import com.craisinlord.anthology.portal.client.PortalEntityTransformationStack;
import com.craisinlord.anthology.portal.client.PortalRenderState;
import com.craisinlord.anthology.portal.PortalEntity;
import com.craisinlord.anthology.portal.PortalRegistry;
import com.craisinlord.anthology.portal.PortalProjection;
import com.craisinlord.anthology.portal.PortalTransforms;
import com.craisinlord.anthology.portal.PortalShape;
import com.craisinlord.anthology.mixins.portal.client.CameraPortalViewAccessor;
import com.craisinlord.anthology.mixins.portal.client.GameRendererPortalAccessor;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class PortalViewRenderer {
    private static final int MAX_VIEW_LEVELS = 2;
    private static final double PORTAL_SEARCH_RADIUS = 128.0D;
    private static final double SURFACE_OFFSET = 0.04D;
    private static final double EXIT_CLIP_OFFSET = 0.01D;
    private static final int APERTURE_SEGMENTS = 32;
    private static final double APERTURE_RIM_FRACTION = 0.92D;
    private static final float FAR_DEPTH = 0.99999F;
    private static final float FAR_WINDOW_DEPTH = 5.0E-6F;
    private static final TextureTarget[] TARGETS = new TextureTarget[MAX_VIEW_LEVELS];
    private static boolean renderingPortalView;
    private static long lastRenderFailureTick = Long.MIN_VALUE;

    private PortalViewRenderer() {
    }

    public static List<RenderPipeline> pipelines() {
        return PortalViewPipelines.all();
    }

    public static boolean isEnabled() {
        return PortalHooks.settings().seeThroughEnabled();
    }

    public static void releaseTargets() {
        PortalVisibilityQueries.clear();
        PortalSkyQueries.clear();
        PortalSectionViews.clear();
        PortalSceneRenderer.releaseSkyRenderers();
        for (int i = 0; i < TARGETS.length; i++) {
            if (TARGETS[i] != null) {
                TARGETS[i].destroyBuffers();
                TARGETS[i] = null;
            }
        }
    }

    public static void close() {
        releaseTargets();
        PortalViewUniforms.clear();
    }

    public static void render(CameraRenderState cameraState, Matrix4f projectionMatrix, DeltaTracker deltaTracker) {
        if (renderingPortalView) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        PortalSectionViews.syncChunkLoading(minecraft);
        if (minecraft.level == null || cameraState == null || !cameraState.initialized || projectionMatrix == null || deltaTracker == null) {
            return;
        }
        PortalViewUniforms.beginFrame();
        Matrix4f viewMatrix = new Matrix4f(cameraState.viewRotationMatrix);
        Matrix4f projection = new Matrix4f(projectionMatrix);
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        try {
            PortalRim.drawHiddenOutlines(minecraft, minecraft.gameRenderer.mainRenderTarget(), cameraState.pos, viewMatrix, projection, partialTick);
        } catch (RuntimeException exception) {
            logRenderFailure(minecraft, exception);
        }
        if (isEnabled()) {
            renderViews(minecraft, cameraState, viewMatrix, projection, deltaTracker, partialTick);
        }
    }

    private static void renderViews(Minecraft minecraft, CameraRenderState cameraState, Matrix4f rootViewMatrix, Matrix4f rootProjection,
                                    DeltaTracker deltaTracker, float partialTick) {
        PortalRenderWatchdog.frame();
        Entity cameraEntity = minecraft.getCameraEntity();
        if (cameraEntity == null || minecraft.gameRenderer == null) {
            return;
        }
        Vec3 cameraPos = cameraState.pos;
        RenderTarget mainTarget = minecraft.gameRenderer.mainRenderTarget();
        int width = mainTarget.width;
        int height = mainTarget.height;

        List<PortalEntity> portals = collectPortals(minecraft, cameraPos);
        if (portals.isEmpty()) {
            return;
        }
        List<RootCandidate> candidates = new ArrayList<>();
        for (PortalEntity portal : portals) {
            boolean custom = PortalClientHooks.customView(portal) != null;
            PortalEntity linked = custom ? null : portal.getLinkedPortal();
            if (!custom && (linked == null || !linked.isAlive()) || !portal.shouldRenderFront(cameraPos)) {
                continue;
            }
            ScreenClip clip = computeScreenClip(cameraPos, rootViewMatrix, rootProjection, portal, partialTick, width, height);
            if (clip != null) {
                candidates.add(new RootCandidate(portal, linked, clip));
            }
        }
        candidates.sort((left, right) -> Integer.compare(right.clip().area(), left.clip().area()));
        int maxRootViews = PortalHooks.settings().maxLiveViews();
        if (candidates.size() > maxRootViews) {
            candidates = candidates.subList(0, maxRootViews);
        }
        if (candidates.isEmpty()) {
            return;
        }
        Matrix4f cameraToWorld = new Matrix4f(rootViewMatrix).invert();
        Vector3f rootLook = cameraToWorld.transformDirection(new Vector3f(0.0F, 0.0F, -1.0F)).normalize();
        Vector3f rootUp = cameraToWorld.transformDirection(new Vector3f(0.0F, 1.0F, 0.0F)).normalize();
        RenderContext rootContext = new RenderContext(cameraPos, new Vec3(rootLook), new Vec3(rootUp), rootViewMatrix);
        ViewBudget budget = new ViewBudget(maxRootViews * 2);
        SceneInputs inputs = new SceneInputs(cameraState, deltaTracker, partialTick, cameraEntity, rootProjection, width, height);
        PortalRenderWatchdog.portalViewDrawn();
        renderingPortalView = true;
        Camera mainCamera = minecraft.gameRenderer.mainCamera();
        try {
            for (RootCandidate candidate : candidates) {
                if (!budget.tryReserve()) {
                    break;
                }
                try {
                    if (candidate.linked() == null) {
                        renderCustomView(minecraft, inputs, rootContext, candidate.portal(), candidate.clip(), mainTarget);
                    } else {
                        renderView(minecraft, inputs, rootContext, rootProjection, candidate.portal(), candidate.linked(), 0,
                                candidate.clip(), mainTarget, budget);
                    }
                } catch (RuntimeException exception) {
                    logRenderFailure(minecraft, exception);
                }
            }
        } finally {
            try {
                ((GameRendererPortalAccessor) minecraft.gameRenderer).anthology$setMainRenderTarget(mainTarget);
                minecraft.getEntityRenderDispatcher().prepare(mainCamera, minecraft.crosshairPickEntity);
                minecraft.getBlockEntityRenderDispatcher().prepare(mainCamera.position());
            } catch (RuntimeException exception) {
                logRenderFailure(minecraft, exception);
            } finally {
                renderingPortalView = false;
            }
        }
    }

    private static void renderView(Minecraft minecraft, SceneInputs inputs, RenderContext context, Matrix4f parentProjection,
                                   PortalEntity source, PortalEntity destination, int parentLevel, ScreenClip clip,
                                   RenderTarget parentTarget, ViewBudget budget) {
        PortalShape exit = destination.getWorldPortalShape();
        if (!PortalTerrain.canView(minecraft, exit.center())) {
            return;
        }
        float partialTick = inputs.partialTick();
        boolean root = parentLevel == 0;
        boolean visible = !root || PortalVisibilityQueries.shouldRender(source.getUUID(), destination.getUUID());
        if (root) {
            drawVisibilityQuery(parentTarget, context, parentProjection, source, destination, partialTick);
        }
        if (!visible) {
            return;
        }
        TextureTarget target = ensureTarget(parentLevel, inputs.width(), inputs.height());
        RenderContext transformed = transformContext(context, source, destination);
        Matrix4f clippedProjection = applyPortalClipPlane(inputs.rootProjection(), transformed, destination);
        GameRendererPortalAccessor targetAccessor = (GameRendererPortalAccessor) minecraft.gameRenderer;
        RenderTarget previousMain = minecraft.gameRenderer.mainRenderTarget();
        targetAccessor.anthology$setMainRenderTarget(target);
        try {
            renderScene(minecraft, inputs, transformed, clippedProjection, source, destination, clip, parentLevel + 1, target,
                    context, parentProjection);
            PortalRim.drawHiddenOutlines(minecraft, target, transformed.cameraPos(), transformed.viewMatrix(), clippedProjection,
                    partialTick, destination);
            if (parentLevel + 1 < MAX_VIEW_LEVELS) {
                renderNestedViews(minecraft, inputs, transformed, clippedProjection, destination, parentLevel + 1, clip, budget, target);
                targetAccessor.anthology$setMainRenderTarget(target);
            }
            PortalRim.drawOverView(target, source, context.cameraPos(), context.viewMatrix(), parentProjection, partialTick);
        } finally {
            targetAccessor.anthology$setMainRenderTarget(previousMain);
        }
        drawPortalComposite(parentTarget, context, parentProjection, source, partialTick, target.getColorTextureView());
    }

    private static void renderCustomView(Minecraft minecraft, SceneInputs inputs, RenderContext context, PortalEntity source,
                                         ScreenClip clip, RenderTarget mainTarget) {
        PortalClientHooks.CustomView view = PortalClientHooks.customView(source);
        if (view == null) {
            return;
        }
        float partialTick = inputs.partialTick();
        TextureTarget target = ensureTarget(0, inputs.width(), inputs.height());
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), new Vector4f(0.0F, 0.0F, 0.0F, 1.0F),
                target.getDepthTexture(), 0.0D, clip.x(), clip.y(), clip.width(), clip.height(), 0);
        view.render(minecraft, target, source.getWorldPortalShape(), context.look(), context.up(), inputs.rootProjection());
        PortalRim.drawOverView(target, source, context.cameraPos(), context.viewMatrix(), inputs.rootProjection(), partialTick);
        drawPortalComposite(mainTarget, context, inputs.rootProjection(), source, partialTick, target.getColorTextureView());
    }

    private static void renderNestedViews(Minecraft minecraft, SceneInputs inputs, RenderContext viewContext, Matrix4f viewProjection,
                                          PortalEntity exit, int level, ScreenClip viewClip, ViewBudget budget, RenderTarget target) {
        PortalShape exitShape = exit.getWorldPortalShape();
        for (PortalEntity child : collectPortals(minecraft, viewContext.cameraPos())) {
            if (!budget.hasRemaining()) {
                return;
            }
            PortalEntity childLinked = child.getLinkedPortal();
            if (child == exit || childLinked == null || !childLinked.isAlive()
                    || !child.shouldRenderFront(viewContext.cameraPos())
                    || !exitShape.intersectsFront(child.getBoundingBox(), 0.0D)) {
                continue;
            }
            ScreenClip childClip = computeScreenClip(viewContext.cameraPos(), viewContext.viewMatrix(), viewProjection,
                    child, inputs.partialTick(), inputs.width(), inputs.height());
            childClip = childClip == null ? null : childClip.intersect(viewClip);
            if (childClip == null || !budget.tryReserve()) {
                continue;
            }
            renderView(minecraft, inputs, viewContext, viewProjection, child, childLinked, level, childClip, target, budget);
        }
    }

    private static void renderScene(Minecraft minecraft, SceneInputs inputs, RenderContext transformed, Matrix4f clippedProjection,
                                    PortalEntity source, PortalEntity destination, ScreenClip clip, int level,
                                    RenderTarget target, RenderContext parentContext, Matrix4f parentProjection) {
        PortalShape exit = destination.getWorldPortalShape();
        float partialTick = inputs.partialTick();
        PortalCamera portalCamera = new PortalCamera();
        portalCamera.setup(minecraft.level, inputs.cameraEntity(), transformed.cameraPos(), transformed.look(), transformed.up());
        Matrix4f cullingProjection = ((CameraPortalViewAccessor) minecraft.gameRenderer.mainCamera()).anthology$createProjectionMatrixForCulling();
        Frustum frustum = new Frustum(transformed.viewMatrix(), cropProjectionToScreenClip(cullingProjection, clip, inputs.width(), inputs.height()));
        frustum.prepare(transformed.cameraPos().x, transformed.cameraPos().y, transformed.cameraPos().z);
        PortalRenderState.pushPortalView(new PortalRenderState.PortalRenderContext(
                source.getWorldPortalShape(), exit, transformed.cameraPos(), transformed.look(), transformed.up(),
                transformed.viewMatrix(), clippedProjection, level, false, destination.getUUID()));
        try {
            PortalTerrain terrain = PortalTerrain.prepare(minecraft, destination.getUUID(), exit.center(), exit.normal(), frustum,
                    transformed.cameraPos());
            PortalSceneRenderer.render(minecraft, target, portalCamera, inputs.cameraState(), inputs.deltaTracker(), partialTick,
                    transformed.viewMatrix(), clippedProjection, inputs.rootProjection(), frustum, terrain,
                    states -> collectCrossingEntities(minecraft, partialTick, source, destination, states),
                    source.getUUID(), destination.getUUID(), level, new int[] {clip.x(), clip.y(), clip.width(), clip.height()},
                    () -> buildAperture(parentContext.cameraPos(), parentContext.viewMatrix(), parentProjection, source, partialTick, false, true));
        } finally {
            PortalRenderState.popPortalView(false);
        }
    }

    private static List<PortalEntity> collectPortals(Minecraft minecraft, Vec3 cameraPos) {
        List<PortalEntity> portals = new ArrayList<>();
        for (PortalEntity portal : PortalRegistry.all(minecraft.level)) {
            if (portal.isAlive() && portal.distanceToSqr(cameraPos) <= PORTAL_SEARCH_RADIUS * PORTAL_SEARCH_RADIUS) {
                portals.add(portal);
            }
        }
        portals.sort((left, right) -> Double.compare(left.distanceToSqr(cameraPos), right.distanceToSqr(cameraPos)));
        return portals;
    }

    private static void logRenderFailure(Minecraft minecraft, RuntimeException exception) {
        long gameTime = minecraft.level == null ? Long.MIN_VALUE : minecraft.level.getGameTime();
        if (lastRenderFailureTick == Long.MIN_VALUE || gameTime == Long.MIN_VALUE || gameTime - lastRenderFailureTick >= 100L) {
            lastRenderFailureTick = gameTime;
            Anthology.LOGGER.error("Portal view render failed", exception);
        }
    }

    private static TextureTarget ensureTarget(int level, int width, int height) {
        TextureTarget target = TARGETS[level];
        if (target == null) {
            target = new TextureTarget("Portal gun view " + level, width, height, GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
            TARGETS[level] = target;
        } else if (target.width != width || target.height != height) {
            target.resize(width, height);
        }
        return target;
    }

    private static RenderContext transformContext(RenderContext context, PortalEntity source, PortalEntity destination) {
        PortalShape sourceShape = source.getWorldPortalShape();
        PortalShape destinationShape = destination.getWorldPortalShape();
        Vec3 relativeEye = context.cameraPos().subtract(sourceShape.center());
        Vec3 destinationEye = destinationShape.center().add(PortalTransforms.transformPosition(sourceShape, destinationShape, relativeEye));
        Vec3 look = PortalTransforms.transformVector(sourceShape, destinationShape, context.look()).normalize();
        Vec3 up = PortalTransforms.transformVector(sourceShape, destinationShape, context.up()).normalize();
        Matrix4f viewMatrix = new Matrix4f().rotation(PortalTransforms.orientationQuaternion(look, up).conjugate(new Quaternionf()));
        return new RenderContext(destinationEye, look, up, viewMatrix);
    }

    private static Matrix4f applyPortalClipPlane(Matrix4f projection, RenderContext context, PortalEntity destination) {
        PortalShape exit = destination.getWorldPortalShape();
        return PortalProjection.clipAtExit(projection, context.cameraPos(), context.look(), context.up(),
                exit.center().add(exit.normal().scale(EXIT_CLIP_OFFSET)), exit.normal(), zeroToOne() ? 0.0F : -1.0F);
    }

    private static ScreenClip computeScreenClip(Vec3 cameraPos, Matrix4f viewMatrix, Matrix4f projectionMatrix,
                                                PortalEntity portal, float partialTick, int width, int height) {
        List<Vector4f> clipVertices = projectPortalPolygon(new Matrix4f(projectionMatrix).mul(viewMatrix), cameraPos, portal, partialTick);
        if (clipVertices.size() < 3) {
            return null;
        }
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (Vector4f projected : clipVertices) {
            float invW = 1.0F / projected.w;
            float screenX = (projected.x * invW * 0.5F + 0.5F) * width;
            float screenY = (projected.y * invW * 0.5F + 0.5F) * height;
            minX = Math.min(minX, screenX);
            minY = Math.min(minY, screenY);
            maxX = Math.max(maxX, screenX);
            maxY = Math.max(maxY, screenY);
        }
        if (maxX <= 0.0F || minX >= width || maxY <= 0.0F || minY >= height) {
            return null;
        }
        int x = Mth.clamp((int) Math.floor(minX), 0, width - 1);
        int maxClipX = Mth.clamp((int) Math.ceil(maxX), x + 1, width);
        int y = Mth.clamp((int) Math.floor(minY), 0, height - 1);
        int maxClipY = Mth.clamp((int) Math.ceil(maxY), y + 1, height);
        return new ScreenClip(x, y, maxClipX - x, maxClipY - y);
    }

    private static Matrix4f cropProjectionToScreenClip(Matrix4f projectionMatrix, ScreenClip clip, int width, int height) {
        float left = (float) (clip.x() * 2.0D / width - 1.0D);
        float right = (float) ((clip.x() + clip.width()) * 2.0D / width - 1.0D);
        float bottom = (float) (clip.y() * 2.0D / height - 1.0D);
        float top = (float) ((clip.y() + clip.height()) * 2.0D / height - 1.0D);
        float widthNdc = Math.max(1.0E-5F, right - left);
        float heightNdc = Math.max(1.0E-5F, top - bottom);
        return new Matrix4f()
                .translate(-(right + left) / widthNdc, -(top + bottom) / heightNdc, 0.0F)
                .scale(2.0F / widthNdc, 2.0F / heightNdc, 1.0F)
                .mul(projectionMatrix);
    }

    private static List<Vector4f> clipPolygon(List<Vector4f> vertices, boolean nearPlane) {
        if (vertices.isEmpty()) {
            return vertices;
        }
        List<Vector4f> clipped = new ArrayList<>(vertices.size() + 2);
        Vector4f previous = vertices.get(vertices.size() - 1);
        float previousDistance = clipPlaneDistance(previous, nearPlane);
        for (Vector4f current : vertices) {
            float currentDistance = clipPlaneDistance(current, nearPlane);
            boolean previousInside = previousDistance >= 0.0F;
            boolean currentInside = currentDistance >= 0.0F;
            if (previousInside != currentInside) {
                float amount = previousDistance / (previousDistance - currentDistance);
                clipped.add(new Vector4f(
                        previous.x + (current.x - previous.x) * amount,
                        previous.y + (current.y - previous.y) * amount,
                        previous.z + (current.z - previous.z) * amount,
                        previous.w + (current.w - previous.w) * amount
                ));
            }
            if (currentInside) {
                clipped.add(new Vector4f(current));
            }
            previous = current;
            previousDistance = currentDistance;
        }
        return clipped;
    }

    private static float clipPlaneDistance(Vector4f vertex, boolean nearPlane) {
        return nearPlane ? vertex.w - vertex.z : vertex.w - 0.001F;
    }

    private static List<Vector4f> projectPortalPolygon(Matrix4f combined, Vec3 cameraPos, PortalEntity portal, float partialTick) {
        Vec3[] corners = apertureOutline(portal, partialTick);
        List<Vector4f> polygon = new ArrayList<>(corners.length);
        for (Vec3 corner : corners) {
            Vec3 relative = corner.subtract(cameraPos);
            polygon.add(new Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1.0F).mul(combined));
        }
        polygon = clipPolygon(polygon, false);
        return clipPolygon(polygon, true);
    }

    static Vec3[] apertureOutline(PortalEntity portal, float partialTick) {
        float scale = portal.getPortalVisualScale(partialTick);
        PortalShape shape = portal.getWorldPortalShape().scaleAperture(scale);
        double radiusHorizontal = Math.min(shape.halfWidth(), (shape.halfWidth() + PortalRim.EXTRA_WIDTH * scale) * APERTURE_RIM_FRACTION);
        double radiusVertical = Math.min(shape.halfHeight(), (shape.halfHeight() + PortalRim.EXTRA_HEIGHT * scale) * APERTURE_RIM_FRACTION);
        return shape.getOutline(SURFACE_OFFSET, APERTURE_SEGMENTS, radiusHorizontal, radiusVertical);
    }

    private static MeshData buildAperture(Vec3 cameraPos, Matrix4f viewMatrix, Matrix4f projectionMatrix,
                                          PortalEntity portal, float partialTick, boolean withScreenUv, boolean farDepth) {
        List<Vector4f> polygon = projectPortalPolygon(new Matrix4f(projectionMatrix).mul(viewMatrix), cameraPos, portal, partialTick);
        if (polygon.size() < 3) {
            return null;
        }
        BufferBuilder buffer = PortalViewDraw.begin(PrimitiveTopology.TRIANGLES,
                withScreenUv ? DefaultVertexFormat.POSITION_TEX : DefaultVertexFormat.POSITION);
        for (int index = 1; index + 1 < polygon.size(); index++) {
            addClipVertex(buffer, polygon.get(0), withScreenUv, farDepth);
            addClipVertex(buffer, polygon.get(index), withScreenUv, farDepth);
            addClipVertex(buffer, polygon.get(index + 1), withScreenUv, farDepth);
        }
        return buffer.build();
    }

    private static void addClipVertex(BufferBuilder buffer, Vector4f clipPosition, boolean withScreenUv, boolean farDepth) {
        float inverseW = 1.0F / clipPosition.w();
        float ndcX = clipPosition.x() * inverseW;
        float ndcY = clipPosition.y() * inverseW;
        float ndcZ = farDepth ? farDepth() : clipPosition.z() * inverseW;
        if (withScreenUv) {
            buffer.addVertex(ndcX, ndcY, ndcZ).setUv(ndcX * 0.5F + 0.5F, ndcY * 0.5F + 0.5F);
        } else {
            buffer.addVertex(ndcX, ndcY, ndcZ);
        }
    }

    private static boolean zeroToOne() {
        return RenderSystem.getDevice().getDeviceInfo().isZZeroToOne();
    }

    private static float farDepth() {
        return zeroToOne() ? FAR_WINDOW_DEPTH : -FAR_DEPTH;
    }

    private static void drawVisibilityQuery(RenderTarget target, RenderContext context, Matrix4f projectionMatrix,
                                            PortalEntity source, PortalEntity destination, float partialTick) {
        MeshData mesh = buildAperture(context.cameraPos(), context.viewMatrix(), projectionMatrix, source, partialTick, false, false);
        if (mesh == null) {
            return;
        }
        boolean[] started = new boolean[1];
        PortalViewDraw.drawClip(target, PortalViewPipelines.PORTAL_QUERY, mesh,
                () -> started[0] = PortalVisibilityQueries.begin(source.getUUID(), destination.getUUID()),
                () -> PortalVisibilityQueries.end(source.getUUID(), destination.getUUID(), started[0]));
    }

    private static void drawPortalComposite(RenderTarget target, RenderContext context, Matrix4f projectionMatrix,
                                            PortalEntity portal, float partialTick, GpuTextureView view) {
        MeshData mesh = buildAperture(context.cameraPos(), context.viewMatrix(), projectionMatrix, portal, partialTick, true, false);
        if (mesh == null) {
            return;
        }
        PortalViewDraw.drawClip(target, PortalViewPipelines.PORTAL_VIEW, mesh, view,
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
    }

    private static void collectCrossingEntities(Minecraft minecraft, float partialTick, PortalEntity sourcePortal,
                                                PortalEntity destinationPortal, List<EntityRenderState> states) {
        List<Entity> entities = minecraft.level.getEntities((Entity) null, sourcePortal.getWorldPortalShape().getBoundsForCulling(),
                entity -> isCrossing(entity, sourcePortal, destinationPortal));
        Set<Entity> collected = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Entity entity : entities) {
            collectCrossingEntityTree(minecraft, partialTick, sourcePortal, destinationPortal, entity, states, collected);
        }
    }

    private static void collectCrossingEntityTree(Minecraft minecraft, float partialTick, PortalEntity sourcePortal,
                                                  PortalEntity destinationPortal, Entity entity, List<EntityRenderState> states,
                                                  Set<Entity> collected) {
        if (!isCrossing(entity, sourcePortal, destinationPortal) || !collected.add(entity)) {
            return;
        }
        PortalEntityTransformationStack transformationStack = new PortalEntityTransformationStack(entity);
        transformationStack.push();
        try {
            Vec3 position = transformationStack.moveEntity(sourcePortal, destinationPortal, partialTick);
            PortalRenderState.pushDestinationShape(destinationPortal.getWorldPortalShape());
            try {
                EntityRenderState state = minecraft.getEntityRenderDispatcher().extractEntity(entity, partialTick);
                state.x = position.x;
                state.y = position.y;
                state.z = position.z;
                states.add(state);
            } finally {
                PortalRenderState.popDestinationShape();
            }
        } finally {
            transformationStack.pop();
        }
        for (Entity passenger : entity.getPassengers()) {
            collectCrossingEntityTree(minecraft, partialTick, sourcePortal, destinationPortal, passenger, states, collected);
        }
    }

    private static boolean isCrossing(Entity entity, PortalEntity sourcePortal, PortalEntity destinationPortal) {
        return entity != null
                && entity.isAlive()
                && !(entity instanceof PortalEntity)
                && entity != sourcePortal
                && entity != destinationPortal
                && entity != Minecraft.getInstance().getCameraEntity()
                && sourcePortal.intersectsEntityBounds(entity);
    }

    private static final class PortalCamera extends Camera {
        private void setup(ClientLevel level, Entity cameraEntity, Vec3 position, Vec3 look, Vec3 up) {
            Vec3 normalizedLook = look.normalize();
            Vec3 normalizedUp = up.subtract(normalizedLook.scale(up.dot(normalizedLook))).normalize();
            this.setLevel(level);
            this.setEntity(cameraEntity);
            this.setPosition(position);
            this.setRotation(PortalTransforms.yawFromLook(normalizedLook), PortalTransforms.pitchFromLook(normalizedLook));
            Quaternionf orientation = PortalTransforms.orientationQuaternion(normalizedLook, normalizedUp);
            this.rotation().set(orientation);
            ((Vector3f) this.forwardVector()).set(0.0F, 0.0F, -1.0F).rotate(orientation);
            ((Vector3f) this.upVector()).set(0.0F, 1.0F, 0.0F).rotate(orientation);
            ((Vector3f) this.leftVector()).set(-1.0F, 0.0F, 0.0F).rotate(orientation);
            CameraPortalViewAccessor state = (CameraPortalViewAccessor) (Object) this;
            state.anthology$setInitialized(true);
            state.anthology$setDetached(true);
            this.attributeProbe().tick(level, position);
        }
    }

    private record SceneInputs(CameraRenderState cameraState, DeltaTracker deltaTracker, float partialTick, Entity cameraEntity,
                               Matrix4f rootProjection, int width, int height) {
    }

    private record RenderContext(Vec3 cameraPos, Vec3 look, Vec3 up, Matrix4f viewMatrix) {
    }

    private record RootCandidate(PortalEntity portal, PortalEntity linked, ScreenClip clip) {
    }

    private static final class ViewBudget {
        private int remaining;

        private ViewBudget(int maximumViews) {
            this.remaining = maximumViews;
        }

        private boolean tryReserve() {
            if (this.remaining <= 0) {
                return false;
            }
            this.remaining--;
            return true;
        }

        private boolean hasRemaining() {
            return this.remaining > 0;
        }
    }

    private record ScreenClip(int x, int y, int width, int height) {
        private int area() {
            return this.width * this.height;
        }

        private ScreenClip intersect(ScreenClip other) {
            int minX = Math.max(this.x, other.x);
            int minY = Math.max(this.y, other.y);
            int maxX = Math.min(this.x + this.width, other.x + other.width);
            int maxY = Math.min(this.y + this.height, other.y + other.height);
            return maxX <= minX || maxY <= minY ? null : new ScreenClip(minX, minY, maxX - minX, maxY - minY);
        }
    }
}
