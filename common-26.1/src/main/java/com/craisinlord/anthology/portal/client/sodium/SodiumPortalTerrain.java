package com.craisinlord.anthology.portal.client.sodium;

import com.craisinlord.anthology.portal.client.PortalRenderState;
import com.craisinlord.anthology.portal.client.PortalTerrain;
import com.craisinlord.anthology.mixins.portal.client.GameRendererPortalAccessor;
import com.craisinlord.anthology.mixins.portal.client.LevelRendererPortalViewAreaAccessor;
import com.craisinlord.anthology.mixins.compat.sodium.RenderSectionManagerAccessor;
import com.craisinlord.anthology.mixins.compat.sodium.SodiumWorldRendererAccessor;
import com.craisinlord.anthology.mixins.compat.sodium.UniformBufferManagerAccessor;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.Iterator;
import java.util.function.Consumer;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation.UploadResourceBudget;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.executor.ChunkJobCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.CoordinateSectionVisitor;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.DeferredTaskList;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.FallbackVisibleChunkCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.storage.SectionStorage;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.tree.RemovableMultiForest;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.render.viewport.ViewportProvider;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.caffeinemc.mods.sodium.client.util.FogStorage;
import net.caffeinemc.mods.sodium.client.util.iterator.ByteIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class SodiumPortalTerrain implements PortalTerrain {
    private static final int FIRST_PORTAL_FRAME = -1_000_000;
    private static final int MAX_PENDING_TASKS = 256;
    private static final TerrainRenderPass[] OPAQUE_PASSES = {DefaultTerrainRenderPasses.SOLID, DefaultTerrainRenderPasses.CUTOUT};
    private static final TerrainRenderPass[] TRANSLUCENT_PASSES = {DefaultTerrainRenderPasses.TRANSLUCENT};
    private static final LongLinkedOpenHashSet PENDING_TASKS = new LongLinkedOpenHashSet();
    private static int nextFrame = FIRST_PORTAL_FRAME;

    private final SodiumWorldRenderer worldRenderer;
    private final RenderSectionManager sectionManager;
    private final SortedRenderLists renderLists;
    private final int sectionCount;
    private final Vec3 cameraPosition;
    private ChunkRenderMatrices matrices;
    private SortedRenderLists previousRenderLists;
    private boolean previousEntityCulling;
    private boolean active;

    private SodiumPortalTerrain(SodiumWorldRenderer worldRenderer, RenderSectionManager sectionManager, SortedRenderLists renderLists,
                                int sectionCount, Vec3 cameraPosition) {
        this.worldRenderer = worldRenderer;
        this.sectionManager = sectionManager;
        this.renderLists = renderLists;
        this.sectionCount = sectionCount;
        this.cameraPosition = cameraPosition;
    }

    public static boolean canView(Minecraft minecraft, Vec3 exitCenter) {
        SodiumWorldRenderer worldRenderer = SodiumWorldRenderer.instanceNullable();
        if (worldRenderer == null || minecraft.player == null
                || ((SodiumWorldRendererAccessor) worldRenderer).anthology$getRenderSectionManager() == null) {
            return false;
        }
        int limit = minecraft.options.getEffectiveRenderDistance();
        return Math.abs(SectionPos.posToSectionCoord(exitCenter.x) - minecraft.player.chunkPosition().x()) <= limit
                && Math.abs(SectionPos.posToSectionCoord(exitCenter.z) - minecraft.player.chunkPosition().z()) <= limit;
    }

    public static PortalTerrain prepare(Minecraft minecraft, Frustum frustum, Vec3 cameraPosition) {
        SodiumWorldRenderer worldRenderer = SodiumWorldRenderer.instanceNullable();
        RenderSectionManager sectionManager = worldRenderer == null ? null
                : ((SodiumWorldRendererAccessor) worldRenderer).anthology$getRenderSectionManager();
        if (sectionManager == null || minecraft.level == null) {
            return EmptyTerrain.INSTANCE;
        }
        RenderSectionManagerAccessor manager = (RenderSectionManagerAccessor) sectionManager;
        int frame = nextFrame--;
        if (nextFrame > FIRST_PORTAL_FRAME) {
            nextFrame = FIRST_PORTAL_FRAME;
        }
        Viewport viewport = ((ViewportProvider) frustum).sodium$createViewport();
        FogParameters fog = ((SodiumWorldRendererAccessor) worldRenderer).anthology$getLastFogParameters();
        float searchDistance = manager.anthology$getSearchDistance(fog != null ? fog : FogParameters.NONE);
        FallbackVisibleChunkCollector collector = new FallbackVisibleChunkCollector(viewport, searchDistance, frame,
                manager.anthology$getRenderSections(), sectionManager.regions, minecraft.level);
        int[] visited = {0};
        CoordinateSectionVisitor apertureFilter = (x, y, z) -> {
            int minX = SectionPos.sectionToBlockCoord(x);
            int minY = SectionPos.sectionToBlockCoord(y);
            int minZ = SectionPos.sectionToBlockCoord(z);
            if (PortalRenderState.shouldRenderBounds(new AABB(minX, minY, minZ, minX + 16, minY + 16, minZ + 16))) {
                collector.visit(x, y, z);
                visited[0]++;
            }
        };
        RemovableMultiForest renderableTree = manager.anthology$getRenderableSectionTree();
        renderableTree.prepareForTraversal();
        renderableTree.traverse(apertureFilter, viewport, searchDistance);
        SortedRenderLists renderLists = collector.createRenderLists(viewport);
        stashPendingTasks(collector.getPendingTaskLists());
        manager.anthology$invalidateRenderLists();
        return new SodiumPortalTerrain(worldRenderer, sectionManager, renderLists, visited[0], cameraPosition);
    }

    public static void submitPendingTasks(RenderSectionManagerAccessor manager, ChunkJobCollector collector, UploadResourceBudget budget) {
        if (PENDING_TASKS.isEmpty()) {
            return;
        }
        SectionStorage sections = manager.anthology$getRenderSections();
        LongIterator iterator = PENDING_TASKS.iterator();
        while (iterator.hasNext() && collector.hasBudgetRemaining() && budget.isAvailable()) {
            RenderSection section = sections.getConsistent(iterator.nextLong());
            if (section != null && !section.isDisposed() && section.getPendingUpdate() != 0) {
                manager.anthology$submitSectionTask(collector, section, budget);
            }
        }
        PENDING_TASKS.clear();
    }

    private static void stashPendingTasks(DeferredTaskList taskList) {
        while (!taskList.isEmpty() && PENDING_TASKS.size() < MAX_PENDING_TASKS) {
            PENDING_TASKS.add(taskList.dequeueNextSectionPos());
        }
    }

    private static GpuSampler sampler(Minecraft minecraft) {
        GpuSampler sampler = ((LevelRendererPortalViewAreaAccessor) minecraft.levelRenderer).anthology$getChunkLayerSampler();
        return sampler != null ? sampler : RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR, true);
    }

    private FogParameters fogParameters(Minecraft minecraft) {
        Object fogRenderer = ((GameRendererPortalAccessor) minecraft.gameRenderer).anthology$getFogRenderer();
        if (fogRenderer instanceof FogStorage storage && storage.sodium$getFogParameters() != null) {
            return storage.sodium$getFogParameters();
        }
        FogParameters fog = ((SodiumWorldRendererAccessor) this.worldRenderer).anthology$getLastFogParameters();
        return fog != null ? fog : FogParameters.NONE;
    }

    @Override
    public int sectionCount() {
        return this.sectionCount;
    }

    @Override
    public void begin(Minecraft minecraft, Matrix4fc viewMatrix, Matrix4fc projection) {
        RenderSectionManagerAccessor manager = (RenderSectionManagerAccessor) this.sectionManager;
        SodiumWorldRendererAccessor renderer = (SodiumWorldRendererAccessor) this.worldRenderer;
        this.matrices = new ChunkRenderMatrices(new Matrix4f(projection), new Matrix4f(viewMatrix));
        this.previousRenderLists = manager.anthology$getRenderLists();
        this.previousEntityCulling = renderer.anthology$getUseEntityCulling();
        manager.anthology$setRenderLists(this.renderLists);
        renderer.anthology$setUseEntityCulling(false);
        this.active = true;
    }

    @Override
    public void end(Minecraft minecraft) {
        if (!this.active) {
            return;
        }
        this.active = false;
        ((RenderSectionManagerAccessor) this.sectionManager).anthology$setRenderLists(this.previousRenderLists);
        ((SodiumWorldRendererAccessor) this.worldRenderer).anthology$setUseEntityCulling(this.previousEntityCulling);
        this.previousRenderLists = null;
    }

    @Override
    public void drawGroup(Minecraft minecraft, ChunkSectionLayerGroup group) {
        TerrainRenderPass[] passes = group == ChunkSectionLayerGroup.OPAQUE ? OPAQUE_PASSES
                : group == ChunkSectionLayerGroup.TRANSLUCENT ? TRANSLUCENT_PASSES : null;
        UniformBufferManager uniforms = ((SodiumWorldRendererAccessor) this.worldRenderer).anthology$getUniformBufferManager();
        if (!this.active || passes == null || uniforms == null) {
            return;
        }
        UniformBufferManagerAccessor uniformState = (UniformBufferManagerAccessor) uniforms;
        boolean previousUpdated = uniformState.anthology$getHasUpdatedThisFrame();
        GpuBufferSlice previousUniforms = uniformState.anthology$getUniformData();
        FogParameters fog = fogParameters(minecraft);
        GpuSampler sampler = sampler(minecraft);
        uniformState.anthology$setHasUpdatedThisFrame(false);
        RenderDevice.enterManagedCode();
        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            for (TerrainRenderPass pass : passes) {
                this.worldRenderer.renderLayer(commandList, this.matrices, pass, this.cameraPosition.x, this.cameraPosition.y,
                        this.cameraPosition.z, fog, sampler);
            }
        } finally {
            RenderDevice.exitManagedCode();
            uniformState.anthology$setHasUpdatedThisFrame(previousUpdated);
            uniformState.anthology$setUniformData(previousUniforms);
        }
    }

    @Override
    public void forEachBlockEntity(Minecraft minecraft, Frustum frustum, Consumer<BlockEntity> consumer) {
        Iterator<ChunkRenderList> lists = this.renderLists.iterator();
        while (lists.hasNext()) {
            ChunkRenderList list = lists.next();
            RenderRegion region = list.getRegion();
            ByteIterator sections = list.sectionsWithEntitiesIterator();
            if (sections == null) {
                continue;
            }
            while (sections.hasNext()) {
                BlockEntity[] blockEntities = region.getCulledBlockEntities(sections.nextByteAsInt());
                if (blockEntities == null) {
                    continue;
                }
                for (BlockEntity blockEntity : blockEntities) {
                    consumer.accept(blockEntity);
                }
            }
        }
        for (RenderSection section : this.sectionManager.getSectionsWithGlobalEntities()) {
            BlockEntity[] blockEntities = section.getRegion().getGlobalBlockEntities(section.getSectionIndex());
            if (blockEntities == null) {
                continue;
            }
            for (BlockEntity blockEntity : blockEntities) {
                if (frustum.isVisible(new AABB(blockEntity.getBlockPos()).inflate(1.0D))) {
                    consumer.accept(blockEntity);
                }
            }
        }
    }

    private enum EmptyTerrain implements PortalTerrain {
        INSTANCE;

        @Override
        public int sectionCount() {
            return 0;
        }

        @Override
        public void begin(Minecraft minecraft, Matrix4fc viewMatrix, Matrix4fc projection) {
        }

        @Override
        public void end(Minecraft minecraft) {
        }

        @Override
        public void drawGroup(Minecraft minecraft, ChunkSectionLayerGroup group) {
        }

        @Override
        public void forEachBlockEntity(Minecraft minecraft, Frustum frustum, Consumer<BlockEntity> consumer) {
        }
    }
}
