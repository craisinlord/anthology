package com.craisinlord.anthology.portal.client.sodium;

import com.craisinlord.anthology.Anthology;
import com.craisinlord.anthology.mixins.compat.sodium.RenderSectionManagerAccessor;
import com.craisinlord.anthology.mixins.compat.sodium.SodiumWorldRendererAccessor;
import com.craisinlord.anthology.mixins.compat.sodium.VisibleChunkCollectorAccessor;
import com.craisinlord.anthology.portal.client.PortalRenderState;
import com.craisinlord.anthology.portal.client.PortalTerrain;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkUpdateType;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import me.jellysquid.mods.sodium.client.render.chunk.lists.VisibleChunkCollector;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class SodiumPortalTerrain implements PortalTerrain {
    private static final double SEED_OFFSET = 0.5D;
    private static final int MAX_PENDING_TASKS_PER_QUEUE = 256;
    private static final Map<ChunkUpdateType, ArrayDeque<RenderSection>> PENDING_TASKS = new EnumMap<>(ChunkUpdateType.class);
    private static boolean disabled;

    private final SodiumWorldRenderer worldRenderer;
    private final RenderSectionManager sectionManager;
    private final SortedRenderLists renderLists;
    private final ObjectArrayList<RenderSection> blockEntitySections;
    private final int sectionCount;
    private SortedRenderLists previousRenderLists;
    private boolean previousEntityCulling;

    private SodiumPortalTerrain(SodiumWorldRenderer worldRenderer, RenderSectionManager sectionManager, SortedRenderLists renderLists,
                                ObjectArrayList<RenderSection> blockEntitySections, int sectionCount) {
        this.worldRenderer = worldRenderer;
        this.sectionManager = sectionManager;
        this.renderLists = renderLists;
        this.blockEntitySections = blockEntitySections;
        this.sectionCount = sectionCount;
    }

    public static boolean canView(Minecraft minecraft, Vec3 exitCenter) {
        if (disabled || minecraft.player == null) {
            return false;
        }
        try {
            if (sectionManager(SodiumWorldRenderer.instanceNullable()) == null) {
                return false;
            }
        } catch (LinkageError exception) {
            disable(exception);
            return false;
        }
        int limit = minecraft.options.getEffectiveRenderDistance();
        return Math.abs(SectionPos.posToSectionCoord(exitCenter.x) - minecraft.player.chunkPosition().x) <= limit
                && Math.abs(SectionPos.posToSectionCoord(exitCenter.z) - minecraft.player.chunkPosition().z) <= limit;
    }

    public static PortalTerrain prepare(Minecraft minecraft, Vec3 exitCenter, Vec3 exitNormal, Frustum frustum) {
        if (disabled) {
            return EmptyTerrain.INSTANCE;
        }
        try {
            return collect(minecraft, exitCenter, exitNormal, frustum);
        } catch (LinkageError exception) {
            disable(exception);
            return EmptyTerrain.INSTANCE;
        }
    }

    private static RenderSectionManager sectionManager(SodiumWorldRenderer worldRenderer) {
        if (!(worldRenderer instanceof SodiumWorldRendererAccessor renderer)) {
            return null;
        }
        RenderSectionManager sectionManager = renderer.anthology$getRenderSectionManager();
        return sectionManager instanceof RenderSectionManagerAccessor ? sectionManager : null;
    }

    private static PortalTerrain collect(Minecraft minecraft, Vec3 exitCenter, Vec3 exitNormal, Frustum frustum) {
        SodiumWorldRenderer worldRenderer = SodiumWorldRenderer.instanceNullable();
        RenderSectionManager sectionManager = sectionManager(worldRenderer);
        if (sectionManager == null || minecraft.level == null) {
            return EmptyTerrain.INSTANCE;
        }
        VisibleChunkCollector collector = new VisibleChunkCollector(0);
        if (!(collector instanceof VisibleChunkCollectorAccessor collectorAccessor)) {
            return EmptyTerrain.INSTANCE;
        }
        Long2ReferenceMap<RenderSection> sections = ((RenderSectionManagerAccessor) sectionManager).anthology$getSectionByPosition();
        RenderSection start = findStart(minecraft, sections, exitCenter, exitNormal);
        if (start == null) {
            return EmptyTerrain.INSTANCE;
        }
        Vec3 cameraPosition = portalCamera(exitCenter);
        double maxDistance = (minecraft.options.getEffectiveRenderDistance() + 1) * 16.0D;
        ObjectArrayList<ChunkRenderList> lists = collectorAccessor.anthology$getSortedRenderLists();
        Reference2ObjectOpenHashMap<RenderRegion, ChunkRenderList> listsByRegion = new Reference2ObjectOpenHashMap<>();
        ObjectArrayList<RenderSection> blockEntitySections = new ObjectArrayList<>();
        ReferenceOpenHashSet<RenderSection> visited = new ReferenceOpenHashSet<>();
        ArrayDeque<RenderSection> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        int count = 0;
        while (!queue.isEmpty()) {
            RenderSection section = queue.poll();
            boolean inView = isInView(section, frustum, cameraPosition, maxDistance);
            if (!inView && section != start) {
                continue;
            }
            if (inView) {
                if (section.getFlags() != 0) {
                    ChunkRenderList list = listsByRegion.get(section.getRegion());
                    if (list == null) {
                        list = new ChunkRenderList(section.getRegion());
                        listsByRegion.put(section.getRegion(), list);
                        lists.add(list);
                    }
                    list.add(section);
                    count++;
                }
                BlockEntity[] blockEntities = section.getCulledBlockEntities();
                if (blockEntities != null && blockEntities.length > 0) {
                    blockEntitySections.add(section);
                }
                stashPendingTask(section);
            }
            visit(section.adjacentDown, visited, queue);
            visit(section.adjacentUp, visited, queue);
            visit(section.adjacentNorth, visited, queue);
            visit(section.adjacentSouth, visited, queue);
            visit(section.adjacentWest, visited, queue);
            visit(section.adjacentEast, visited, queue);
        }
        return new SodiumPortalTerrain(worldRenderer, sectionManager, collector.createRenderLists(), blockEntitySections, count);
    }

    private static RenderSection findStart(Minecraft minecraft, Long2ReferenceMap<RenderSection> sections, Vec3 exitCenter, Vec3 exitNormal) {
        Vec3 seed = exitCenter.add(exitNormal.normalize().scale(SEED_OFFSET));
        RenderSection start = sectionAt(minecraft, sections, seed);
        return start != null ? start : sectionAt(minecraft, sections, exitCenter);
    }

    private static RenderSection sectionAt(Minecraft minecraft, Long2ReferenceMap<RenderSection> sections, Vec3 position) {
        if (position.y < minecraft.level.getMinBuildHeight() || position.y >= minecraft.level.getMaxBuildHeight()) {
            return null;
        }
        RenderSection section = sections.get(SectionPos.asLong(SectionPos.posToSectionCoord(position.x),
                SectionPos.posToSectionCoord(position.y), SectionPos.posToSectionCoord(position.z)));
        return section == null || section.isDisposed() ? null : section;
    }

    private static Vec3 portalCamera(Vec3 fallback) {
        PortalRenderState.PortalRenderContext context = PortalRenderState.getContext();
        return context == null ? fallback : context.cameraPos();
    }

    private static boolean isInView(RenderSection section, Frustum frustum, Vec3 cameraPosition, double maxDistance) {
        int minX = section.getOriginX();
        int minY = section.getOriginY();
        int minZ = section.getOriginZ();
        double dx = Math.max(Math.max(minX - cameraPosition.x, cameraPosition.x - (minX + 16)), 0.0D);
        double dz = Math.max(Math.max(minZ - cameraPosition.z, cameraPosition.z - (minZ + 16)), 0.0D);
        if (dx * dx + dz * dz > maxDistance * maxDistance) {
            return false;
        }
        AABB bounds = new AABB(minX, minY, minZ, minX + 16, minY + 16, minZ + 16);
        return frustum.isVisible(bounds) && PortalRenderState.shouldRenderBounds(bounds);
    }

    private static void visit(RenderSection neighbour, Set<RenderSection> visited, ArrayDeque<RenderSection> queue) {
        if (neighbour != null && !neighbour.isDisposed() && visited.add(neighbour)) {
            queue.add(neighbour);
        }
    }

    private static void stashPendingTask(RenderSection section) {
        ChunkUpdateType type = section.getPendingUpdate();
        if (type == null || section.getBuildCancellationToken() != null) {
            return;
        }
        ArrayDeque<RenderSection> pending = PENDING_TASKS.computeIfAbsent(type, key -> new ArrayDeque<>());
        if (pending.size() < MAX_PENDING_TASKS_PER_QUEUE) {
            pending.add(section);
        }
    }

    public static void drainPendingTasks(Map<ChunkUpdateType, ArrayDeque<RenderSection>> rebuildLists) {
        if (PENDING_TASKS.isEmpty()) {
            return;
        }
        if (rebuildLists != null) {
            for (Map.Entry<ChunkUpdateType, ArrayDeque<RenderSection>> entry : PENDING_TASKS.entrySet()) {
                ChunkUpdateType type = entry.getKey();
                ArrayDeque<RenderSection> queue = rebuildLists.get(type);
                if (queue == null) {
                    continue;
                }
                int limit = type.getMaximumQueueSize();
                ReferenceOpenHashSet<RenderSection> queued = new ReferenceOpenHashSet<>(queue);
                for (RenderSection section : entry.getValue()) {
                    if (queue.size() >= limit) {
                        break;
                    }
                    if (!section.isDisposed() && section.getPendingUpdate() == type
                            && section.getBuildCancellationToken() == null && queued.add(section)) {
                        queue.add(section);
                    }
                }
            }
        }
        PENDING_TASKS.clear();
    }

    private static void disable(Throwable exception) {
        if (!disabled) {
            disabled = true;
            PENDING_TASKS.clear();
            Anthology.LOGGER.warn("Unsupported Sodium or Embeddium version, portal views fall back to rim and surface", exception);
        }
    }

    @Override
    public int sectionCount() {
        return this.sectionCount;
    }

    @Override
    public void begin(Minecraft minecraft) {
        RenderSectionManagerAccessor manager = (RenderSectionManagerAccessor) this.sectionManager;
        SodiumWorldRendererAccessor renderer = (SodiumWorldRendererAccessor) this.worldRenderer;
        this.previousRenderLists = manager.anthology$getRenderLists();
        this.previousEntityCulling = renderer.anthology$getUseEntityCulling();
        manager.anthology$setRenderLists(this.renderLists);
        renderer.anthology$setUseEntityCulling(false);
    }

    @Override
    public void end(Minecraft minecraft) {
        ((RenderSectionManagerAccessor) this.sectionManager).anthology$setRenderLists(this.previousRenderLists);
        ((SodiumWorldRendererAccessor) this.worldRenderer).anthology$setUseEntityCulling(this.previousEntityCulling);
    }

    @Override
    public void forEachBlockEntity(Minecraft minecraft, Frustum frustum, Consumer<BlockEntity> consumer) {
        if (disabled) {
            return;
        }
        try {
            for (RenderSection section : this.blockEntitySections) {
                BlockEntity[] blockEntities = section.getCulledBlockEntities();
                if (blockEntities == null) {
                    continue;
                }
                for (BlockEntity blockEntity : blockEntities) {
                    consumer.accept(blockEntity);
                }
            }
            for (RenderSection section : this.sectionManager.getSectionsWithGlobalEntities()) {
                BlockEntity[] blockEntities = section.getGlobalBlockEntities();
                if (blockEntities == null) {
                    continue;
                }
                for (BlockEntity blockEntity : blockEntities) {
                    if (frustum.isVisible(new AABB(blockEntity.getBlockPos()).inflate(1.0D))) {
                        consumer.accept(blockEntity);
                    }
                }
            }
        } catch (LinkageError exception) {
            disable(exception);
        }
    }

    @Override
    public void drawLayer(Minecraft minecraft, RenderType renderType, Vec3 cameraPosition, Matrix4f viewMatrix, Matrix4f projection) {
        TerrainRenderPass[] passes;
        if (renderType == RenderType.solid()) {
            passes = new TerrainRenderPass[] {DefaultTerrainRenderPasses.SOLID, DefaultTerrainRenderPasses.CUTOUT};
        } else if (renderType == RenderType.translucent()) {
            passes = new TerrainRenderPass[] {DefaultTerrainRenderPasses.TRANSLUCENT};
        } else {
            return;
        }
        if (disabled) {
            return;
        }
        try {
            ChunkRenderMatrices matrices = new ChunkRenderMatrices(projection, viewMatrix);
            RenderDevice.enterManagedCode();
            try {
                for (TerrainRenderPass pass : passes) {
                    this.sectionManager.renderLayer(matrices, pass, cameraPosition.x, cameraPosition.y, cameraPosition.z);
                }
            } finally {
                RenderDevice.exitManagedCode();
            }
        } catch (LinkageError exception) {
            disable(exception);
        }
    }

    private enum EmptyTerrain implements PortalTerrain {
        INSTANCE;

        @Override
        public int sectionCount() {
            return 0;
        }

        @Override
        public void begin(Minecraft minecraft) {
        }

        @Override
        public void end(Minecraft minecraft) {
        }

        @Override
        public void drawLayer(Minecraft minecraft, RenderType renderType, Vec3 cameraPosition, Matrix4f viewMatrix, Matrix4f projection) {
        }

        @Override
        public void forEachBlockEntity(Minecraft minecraft, Frustum frustum, Consumer<BlockEntity> consumer) {
        }
    }
}
