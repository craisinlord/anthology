package com.craisinlord.anthology.portal.client;

import com.craisinlord.anthology.mixins.portal.client.LevelRendererPortalViewAreaAccessor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class PortalSectionViews {
    private static final int MAX_VIEWS = 8;
    private static final double SEED_OFFSET = 0.5D;
    private static final LinkedHashMap<UUID, View> VIEWS = new LinkedHashMap<>(8, 0.75F, true);
    private static volatile SectionOcclusionGraph[] graphs = new SectionOcclusionGraph[0];
    private static ViewArea ownerArea;
    private static int mainSectionX = Integer.MIN_VALUE;
    private static int mainSectionY = Integer.MIN_VALUE;
    private static int mainSectionZ = Integer.MIN_VALUE;

    private PortalSectionViews() {
    }

    public static boolean canView(Minecraft minecraft, Vec3 exitCenter) {
        ViewArea area = ((LevelRendererPortalViewAreaAccessor) minecraft.levelRenderer).anthology$getViewArea();
        if (area == null) {
            return false;
        }
        LevelRendererPortalViewAreaAccessor accessor = (LevelRendererPortalViewAreaAccessor) minecraft.levelRenderer;
        int limit = area.getViewDistance() - 1;
        return Math.abs(SectionPos.posToSectionCoord(exitCenter.x) - accessor.anthology$getLastCameraSectionX()) <= limit
                && Math.abs(SectionPos.posToSectionCoord(exitCenter.z) - accessor.anthology$getLastCameraSectionZ()) <= limit;
    }

    public static View prepare(Minecraft minecraft, UUID exitId, Vec3 exitCenter, Vec3 exitNormal, Frustum frustum) {
        LevelRendererPortalViewAreaAccessor accessor = (LevelRendererPortalViewAreaAccessor) minecraft.levelRenderer;
        ViewArea area = accessor.anthology$getViewArea();
        if (area != ownerArea) {
            clear();
            ownerArea = area;
        }
        int sectionX = accessor.anthology$getLastCameraSectionX();
        int sectionY = accessor.anthology$getLastCameraSectionY();
        int sectionZ = accessor.anthology$getLastCameraSectionZ();
        if (sectionX != mainSectionX || sectionY != mainSectionY || sectionZ != mainSectionZ) {
            mainSectionX = sectionX;
            mainSectionY = sectionY;
            mainSectionZ = sectionZ;
            for (View view : VIEWS.values()) {
                view.graph.invalidate();
            }
        }
        View view = VIEWS.get(exitId);
        if (view == null) {
            view = new View(area);
            VIEWS.put(exitId, view);
            trim();
            publishGraphs();
        }
        Vec3 seed = exitCenter.add(exitNormal.normalize().scale(SEED_OFFSET));
        if (!seed.equals(view.seed)) {
            view.seed = seed;
            view.seedCamera = new SeedCamera(seed);
            view.updateFrustum = centredFrustum(seed);
            view.graph.invalidate();
        }
        view.partialUpdates.clear();
        view.graph.update(minecraft.smartCull, view.seedCamera, view.updateFrustum(), view.partialUpdates,
                minecraft.level.getChunkSource().getLoadedEmptySections());
        view.visibleSections.clear();
        view.nearbySections.clear();
        view.graph.addSectionsInFrustum(frustum, view.visibleSections, view.nearbySections);
        return view;
    }

    public static void onSectionCompiled(SectionRenderDispatcher.RenderSection section) {
        for (SectionOcclusionGraph graph : graphs) {
            graph.schedulePropagationFrom(section);
        }
    }

    public static void onChunkLoaded(ChunkPos pos) {
        for (SectionOcclusionGraph graph : graphs) {
            graph.onChunkReadyToRender(pos);
        }
    }

    public static void onChunkDropped(ChunkPos pos) {
        for (SectionOcclusionGraph graph : graphs) {
            graph.invalidate();
        }
    }

    public static void clear() {
        for (View view : VIEWS.values()) {
            view.release();
        }
        VIEWS.clear();
        publishGraphs();
        ownerArea = null;
        mainSectionX = Integer.MIN_VALUE;
        mainSectionY = Integer.MIN_VALUE;
        mainSectionZ = Integer.MIN_VALUE;
    }

    private static void publishGraphs() {
        graphs = VIEWS.values().stream().map(view -> view.graph).toArray(SectionOcclusionGraph[]::new);
    }

    private static void trim() {
        Iterator<Map.Entry<UUID, View>> iterator = VIEWS.entrySet().iterator();
        while (VIEWS.size() > MAX_VIEWS && iterator.hasNext()) {
            View view = iterator.next().getValue();
            iterator.remove();
            view.release();
        }
    }

    public static final class View {
        private final SectionOcclusionGraph graph = new SectionOcclusionGraph();
        private final List<SectionRenderDispatcher.RenderSection> partialUpdates = new ArrayList<>();
        private final ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections = new ObjectArrayList<>();
        private final List<SectionRenderDispatcher.RenderSection> nearbySections = new ArrayList<>();
        private Vec3 seed;
        private Camera seedCamera;
        private Frustum updateFrustum;

        private View(ViewArea area) {
            this.graph.waitAndReset(area);
        }

        public ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections() {
            return this.visibleSections;
        }

        private Frustum updateFrustum() {
            return this.updateFrustum;
        }

        private void release() {
            this.graph.waitAndReset(null);
        }
    }

    private static Frustum centredFrustum(Vec3 position) {
        Frustum frustum = new Frustum(new Matrix4f(), new Matrix4f().setPerspective((float) Math.toRadians(90.0D), 1.0F, 0.05F, 1024.0F));
        frustum.prepare(position.x, position.y, position.z);
        return frustum;
    }

    private static final class SeedCamera extends Camera {
        private SeedCamera(Vec3 position) {
            this.setPosition(position);
        }
    }
}
