package com.craisinlord.anthology.portal.client;

import com.craisinlord.anthology.mixins.portal.client.SectionOcclusionGraphPortalAccessor;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ChunkLoadingRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class PortalSectionViews {
    private static final int MAX_VIEWS = 8;
    private static final double SEED_OFFSET = 0.5D;
    private static final LinkedHashMap<UUID, View> VIEWS = new LinkedHashMap<>(8, 0.75F, true);
    private static boolean collectingSections;
    private static volatile SectionOcclusionGraph[] graphs = new SectionOcclusionGraph[0];
    private static ViewArea ownerArea;
    private static int mainSectionX = Integer.MIN_VALUE;
    private static int mainSectionY = Integer.MIN_VALUE;
    private static int mainSectionZ = Integer.MIN_VALUE;

    private PortalSectionViews() {
    }

    public static boolean canView(Minecraft minecraft, Vec3 exitCenter) {
        ViewArea area = minecraft.levelRenderer.viewArea();
        if (area == null) {
            return false;
        }
        SectionPos cameraSection = area.getCameraSectionPos();
        int limit = area.getViewDistance() - 1;
        return Math.abs(SectionPos.posToSectionCoord(exitCenter.x) - cameraSection.x()) <= limit
                && Math.abs(SectionPos.posToSectionCoord(exitCenter.z) - cameraSection.z()) <= limit;
    }

    public static View prepare(Minecraft minecraft, UUID exitId, Vec3 exitCenter, Vec3 exitNormal, Frustum frustum) {
        ViewArea area = minecraft.levelRenderer.viewArea();
        if (area != ownerArea) {
            clear();
            ownerArea = area;
        }
        SectionPos cameraSection = area.getCameraSectionPos();
        int sectionX = cameraSection.x();
        int sectionY = cameraSection.y();
        int sectionZ = cameraSection.z();
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
            view = new View(area, minecraft.levelRenderer.sectionOcclusionGraph());
            VIEWS.put(exitId, view);
            trim();
            publishGraphs();
        }
        Vec3 seed = exitCenter.add(exitNormal.normalize().scale(SEED_OFFSET));
        if (!seed.equals(view.seed)) {
            view.seed = seed;
            view.seedCamera.pos = seed;
            view.seedCamera.blockPos = BlockPos.containing(seed);
            view.seedCamera.cullFrustum = centredFrustum(seed);
            view.graph.invalidate();
        }
        view.seedCamera.smartCull = minecraft.smartCull;
        view.updateExpectedChunks(minecraft.level.getChunkSource());
        view.graph.update(view.seedCamera, minecraft.options.fov().get(), view.chunkLoading);
        view.visibleSections.clear();
        view.nearbySections.clear();
        collectingSections = true;
        try {
            view.graph.addSectionsInFrustum(frustum, view.visibleSections, view.nearbySections);
        } finally {
            collectingSections = false;
        }
        return view;
    }

    public static boolean isCollectingSections() {
        return collectingSections;
    }

    public static void syncChunkLoading(Minecraft minecraft) {
        if (VIEWS.isEmpty() || minecraft.gameRenderer == null) {
            return;
        }
        ChunkLoadingRenderState state = minecraft.gameRenderer.gameRenderState().levelRenderState.chunkLoadingRenderState;
        for (View view : VIEWS.values()) {
            view.graph.updateLoadedChunks(state.addedLoadedChunks, state.removedLoadedChunks);
            view.graph.updateEmptySections(state.addedEmptySections, state.removedEmptySections);
        }
    }

    public static void onPropagationScheduled(SectionOcclusionGraph source, SectionRenderDispatcher.RenderSection section) {
        SectionOcclusionGraph[] current = graphs;
        for (SectionOcclusionGraph graph : current) {
            if (graph == source) {
                return;
            }
        }
        for (SectionOcclusionGraph graph : current) {
            try {
                graph.schedulePropagationFrom(section);
            } catch (RuntimeException ignored) {
            }
        }
    }

    public static void onChunkDropped(ChunkPos pos) {
        for (SectionOcclusionGraph graph : graphs) {
            graph.invalidate();
        }
    }

    public static void clear() {
        List<View> released = new ArrayList<>(VIEWS.values());
        VIEWS.clear();
        publishGraphs();
        for (View view : released) {
            view.release();
        }
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
            publishGraphs();
            view.release();
        }
    }

    public static final class View {
        private final SectionOcclusionGraph graph = new SectionOcclusionGraph();
        private final ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections = new ObjectArrayList<>();
        private final List<SectionRenderDispatcher.RenderSection> nearbySections = new ArrayList<>();
        private final CameraRenderState seedCamera = new CameraRenderState();
        private final ChunkLoadingRenderState chunkLoading = new ChunkLoadingRenderState();
        private Vec3 seed;

        private View(ViewArea area, SectionOcclusionGraph mainGraph) {
            this.graph.waitAndReset(area);
            SectionOcclusionGraphPortalAccessor main = (SectionOcclusionGraphPortalAccessor) mainGraph;
            this.graph.updateLoadedChunks(main.anthology$getLoadedChunks().clone(), new LongOpenHashSet());
            this.graph.updateEmptySections(main.anthology$getEmptySections().clone(), new LongOpenHashSet());
        }

        public ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections() {
            return this.visibleSections;
        }

        private void updateExpectedChunks(ClientChunkCache chunkCache) {
            this.chunkLoading.loadedExpectedChunks.clear();
            this.graph.expectedChunks().forEach(chunk -> {
                if (chunkCache.hasChunk(ChunkPos.getX(chunk), ChunkPos.getZ(chunk))) {
                    this.chunkLoading.loadedExpectedChunks.add(chunk);
                }
            });
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
}
