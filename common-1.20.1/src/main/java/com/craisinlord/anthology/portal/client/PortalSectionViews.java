package com.craisinlord.anthology.portal.client;

import com.craisinlord.anthology.mixins.portal.client.LevelRendererPortalViewAreaAccessor;
import com.craisinlord.anthology.mixins.portal.client.ViewAreaPortalInvoker;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;

public final class PortalSectionViews {
    private static final int MAX_VIEWS = 8;
    private static final double SEED_OFFSET = 0.5D;
    private static final long CHANGE_REFRESH_NANOS = 250_000_000L;
    private static final long INCOMPLETE_REFRESH_NANOS = 500_000_000L;
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final LinkedHashMap<UUID, View> VIEWS = new LinkedHashMap<>(8, 0.75F, true);
    private static final AtomicLong GENERATION = new AtomicLong();
    private static ViewArea ownerArea;
    private static int mainSectionX = Integer.MIN_VALUE;
    private static int mainSectionY = Integer.MIN_VALUE;
    private static int mainSectionZ = Integer.MIN_VALUE;

    private PortalSectionViews() {
    }

    public static boolean canView(Minecraft minecraft, Vec3 exitCenter) {
        LevelRendererPortalViewAreaAccessor accessor = (LevelRendererPortalViewAreaAccessor) minecraft.levelRenderer;
        ViewArea area = accessor.anthology$getViewArea();
        if (area == null) {
            return false;
        }
        int limit = (int) minecraft.levelRenderer.getLastViewDistance() - 1;
        return Math.abs(SectionPos.posToSectionCoord(exitCenter.x) - accessor.anthology$getLastCameraChunkX()) <= limit
                && Math.abs(SectionPos.posToSectionCoord(exitCenter.z) - accessor.anthology$getLastCameraChunkZ()) <= limit;
    }

    public static View prepare(Minecraft minecraft, UUID exitId, Vec3 exitCenter, Vec3 exitNormal, Frustum frustum) {
        LevelRendererPortalViewAreaAccessor accessor = (LevelRendererPortalViewAreaAccessor) minecraft.levelRenderer;
        ViewArea area = accessor.anthology$getViewArea();
        if (area != ownerArea) {
            clear();
            ownerArea = area;
        }
        int sectionX = accessor.anthology$getLastCameraChunkX();
        int sectionY = accessor.anthology$getLastCameraChunkY();
        int sectionZ = accessor.anthology$getLastCameraChunkZ();
        if (sectionX != mainSectionX || sectionY != mainSectionY || sectionZ != mainSectionZ) {
            mainSectionX = sectionX;
            mainSectionY = sectionY;
            mainSectionZ = sectionZ;
            for (View view : VIEWS.values()) {
                view.invalidate();
            }
        }
        View view = VIEWS.get(exitId);
        if (view == null) {
            view = new View();
            VIEWS.put(exitId, view);
            trim();
        }
        Vec3 seed = exitCenter.add(exitNormal.normalize().scale(SEED_OFFSET));
        if (!seed.equals(view.seed)) {
            view.seed = seed;
            view.invalidate();
        }
        if (area != null && view.needsRebuild(System.nanoTime(), GENERATION.get())) {
            view.rebuild(minecraft, area, BlockPos.containing(seed), sectionX, sectionY, sectionZ);
        }
        view.visibleSections.clear();
        for (ChunkRenderDispatcher.RenderChunk section : view.graphSections) {
            if (frustum.isVisible(section.getBoundingBox())) {
                view.visibleSections.add(section);
            }
        }
        return view;
    }

    public static void onSectionCompiled(ChunkRenderDispatcher.RenderChunk section) {
        GENERATION.incrementAndGet();
    }

    public static void onChunkDropped(int chunkX, int chunkZ) {
        GENERATION.incrementAndGet();
    }

    public static void clear() {
        VIEWS.clear();
        ownerArea = null;
        mainSectionX = Integer.MIN_VALUE;
        mainSectionY = Integer.MIN_VALUE;
        mainSectionZ = Integer.MIN_VALUE;
    }

    private static void trim() {
        Iterator<Map.Entry<UUID, View>> iterator = VIEWS.entrySet().iterator();
        while (VIEWS.size() > MAX_VIEWS && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    public static final class View {
        private final ObjectArrayList<ChunkRenderDispatcher.RenderChunk> graphSections = new ObjectArrayList<>();
        private final ObjectArrayList<ChunkRenderDispatcher.RenderChunk> visibleSections = new ObjectArrayList<>();
        private Vec3 seed;
        private boolean valid;
        private boolean incomplete;
        private long builtNanos;
        private long builtGeneration = -1L;

        private View() {
        }

        public ObjectArrayList<ChunkRenderDispatcher.RenderChunk> visibleSections() {
            return this.visibleSections;
        }

        private void invalidate() {
            this.valid = false;
        }

        private boolean needsRebuild(long now, long generation) {
            if (!this.valid) {
                return true;
            }
            long age = now - this.builtNanos;
            if (generation != this.builtGeneration && age >= CHANGE_REFRESH_NANOS) {
                return true;
            }
            return this.incomplete && age >= INCOMPLETE_REFRESH_NANOS;
        }

        private void rebuild(Minecraft minecraft, ViewArea area, BlockPos seedPos, int cameraSectionX, int cameraSectionY, int cameraSectionZ) {
            this.builtGeneration = GENERATION.get();
            this.builtNanos = System.nanoTime();
            this.valid = true;
            this.incomplete = false;
            this.graphSections.clear();
            ClientLevel level = minecraft.level;
            if (level == null || seedPos.getY() < level.getMinBuildHeight() || seedPos.getY() >= level.getMaxBuildHeight()) {
                return;
            }
            ViewAreaPortalInvoker sections = (ViewAreaPortalInvoker) area;
            ChunkRenderDispatcher.RenderChunk start = sections.anthology$getRenderChunkAt(seedPos);
            BlockPos startOrigin = new BlockPos(SectionPos.blockToSectionCoord(seedPos.getX()) << 4,
                    SectionPos.blockToSectionCoord(seedPos.getY()) << 4, SectionPos.blockToSectionCoord(seedPos.getZ()) << 4);
            if (start == null || !start.getOrigin().equals(startOrigin)) {
                return;
            }
            int range = (int) minecraft.levelRenderer.getLastViewDistance() * 16;
            BlockPos cameraOrigin = new BlockPos(cameraSectionX << 4, cameraSectionY << 4, cameraSectionZ << 4);
            boolean smartCull = minecraft.smartCull;
            Map<ChunkRenderDispatcher.RenderChunk, Node> visited = new IdentityHashMap<>();
            ArrayDeque<Node> queue = new ArrayDeque<>();
            Node first = new Node(start, 0, 0);
            visited.put(start, first);
            queue.add(first);
            while (!queue.isEmpty()) {
                Node node = queue.poll();
                ChunkRenderDispatcher.RenderChunk chunk = node.chunk;
                this.graphSections.add(chunk);
                for (Direction direction : DIRECTIONS) {
                    if (smartCull && (node.directions & bit(direction.getOpposite())) != 0) {
                        continue;
                    }
                    ChunkRenderDispatcher.RenderChunk neighbor = relative(sections, level, cameraOrigin, range, chunk, direction);
                    if (neighbor == null) {
                        continue;
                    }
                    if (smartCull && node.sources != 0 && !canSeeThrough(chunk, node.sources, direction)) {
                        continue;
                    }
                    Node existing = visited.get(neighbor);
                    if (existing != null) {
                        existing.sources |= bit(direction);
                        continue;
                    }
                    if (!neighbor.hasAllNeighbors()) {
                        this.incomplete = true;
                        continue;
                    }
                    Node next = new Node(neighbor, bit(direction), node.directions | bit(direction));
                    visited.put(neighbor, next);
                    queue.add(next);
                }
            }
        }
    }

    private static boolean canSeeThrough(ChunkRenderDispatcher.RenderChunk chunk, int sources, Direction direction) {
        ChunkRenderDispatcher.CompiledChunk compiled = chunk.getCompiledChunk();
        for (int index = 0; index < DIRECTIONS.length; index++) {
            if ((sources & (1 << index)) != 0 && compiled.facesCanSeeEachother(DIRECTIONS[index].getOpposite(), direction)) {
                return true;
            }
        }
        return false;
    }

    private static ChunkRenderDispatcher.RenderChunk relative(ViewAreaPortalInvoker sections, ClientLevel level, BlockPos cameraOrigin,
                                                             int range, ChunkRenderDispatcher.RenderChunk chunk, Direction direction) {
        BlockPos origin = chunk.getRelativeOrigin(direction);
        if (Math.abs(cameraOrigin.getX() - origin.getX()) > range
                || Math.abs(cameraOrigin.getY() - origin.getY()) > range
                || Math.abs(cameraOrigin.getZ() - origin.getZ()) > range
                || origin.getY() < level.getMinBuildHeight()
                || origin.getY() >= level.getMaxBuildHeight()) {
            return null;
        }
        ChunkRenderDispatcher.RenderChunk neighbor = sections.anthology$getRenderChunkAt(origin);
        return neighbor == null || !neighbor.getOrigin().equals(origin) ? null : neighbor;
    }

    private static int bit(Direction direction) {
        return 1 << direction.ordinal();
    }

    private static final class Node {
        private final ChunkRenderDispatcher.RenderChunk chunk;
        private final int directions;
        private int sources;

        private Node(ChunkRenderDispatcher.RenderChunk chunk, int sources, int directions) {
            this.chunk = chunk;
            this.sources = sources;
            this.directions = directions;
        }
    }
}
