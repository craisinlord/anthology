package com.craisinlord.anthology.portal.client;

import com.craisinlord.anthology.portal.client.PortalRenderState;
import com.craisinlord.anthology.mixins.portal.client.LevelExtractorPortalAccessor;
import com.craisinlord.anthology.mixins.portal.client.LevelRendererPortalViewAreaAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

final class VanillaPortalTerrain implements PortalTerrain {
    private static final int MAX_REBUILDS_PER_SCENE = 32;
    private static final float MIN_VISIBILITY = 0.3F;

    private final ObjectArrayList<SectionRenderDispatcher.RenderSection> sections;
    private ChunkSectionsToRender chunkSections;

    private VanillaPortalTerrain(ObjectArrayList<SectionRenderDispatcher.RenderSection> sections) {
        this.sections = sections;
    }

    static VanillaPortalTerrain prepare(Minecraft minecraft, UUID exitId, Vec3 exitCenter, Vec3 exitNormal, Frustum frustum, Vec3 cameraPosition) {
        PortalSectionViews.View view = PortalSectionViews.prepare(minecraft, exitId, exitCenter, exitNormal, frustum);
        ObjectArrayList<SectionRenderDispatcher.RenderSection> sections = new ObjectArrayList<>(view.visibleSections());
        sections.removeIf(section -> !PortalRenderState.shouldRenderBounds(section.getBoundingBox()));
        sections.sort(Comparator.comparingDouble(section -> section.getBoundingBox().getCenter().distanceToSqr(cameraPosition)));
        queueDirtySections(minecraft, sections);
        return new VanillaPortalTerrain(sections);
    }

    @Override
    public int sectionCount() {
        return this.sections.size();
    }

    @Override
    public void begin(Minecraft minecraft, Matrix4fc viewMatrix, Matrix4fc projection) {
        this.chunkSections = buildDraws(minecraft, viewMatrix);
    }

    @Override
    public void end(Minecraft minecraft) {
        this.chunkSections = null;
    }

    @Override
    public void drawGroup(Minecraft minecraft, ChunkSectionLayerGroup group, RenderPass renderPass) {
        if (this.chunkSections != null) {
            GpuTextureView blockAtlas = minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
            this.chunkSections.renderGroup(group, renderPass, sampler(minecraft), blockAtlas, false);
        }
    }

    @Override
    public void forEachBlockEntity(Minecraft minecraft, Frustum frustum, BiConsumer<BlockEntity, Boolean> consumer) {
        long now = Util.getMillis();
        long fadeDuration = fadeDuration(minecraft);
        for (SectionRenderDispatcher.RenderSection section : this.sections) {
            List<BlockEntity> blockEntities = section.getSectionMesh().getRenderableBlockEntities();
            if (blockEntities.isEmpty() || section.getVisibility(now, fadeDuration) < MIN_VISIBILITY) {
                continue;
            }
            for (BlockEntity blockEntity : blockEntities) {
                consumer.accept(blockEntity, false);
            }
        }
        for (BlockEntity blockEntity : List.copyOf(minecraft.level.getGloballyRenderedBlockEntities())) {
            if (!blockEntity.isRemoved() && frustum.isVisible(new AABB(blockEntity.getBlockPos()).inflate(1.0D))) {
                consumer.accept(blockEntity, true);
            }
        }
    }

    private static GpuSampler sampler(Minecraft minecraft) {
        GpuSampler sampler = ((LevelRendererPortalViewAreaAccessor) minecraft.levelRenderer).anthology$getChunkLayerSampler();
        return sampler != null ? sampler : RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR, true);
    }

    private static long fadeDuration(Minecraft minecraft) {
        return Util.toMillis(minecraft.gameRenderer.gameRenderState().optionsRenderState.chunkSectionFadeInTime);
    }

    private ChunkSectionsToRender buildDraws(Minecraft minecraft, Matrix4fc viewMatrix) {
        EnumMap<ChunkSectionLayer, List<DrawGroup>> drawGroups = new EnumMap<>(ChunkSectionLayer.class);
        Int2ObjectOpenHashMap<DrawGroup> groupCache = new Int2ObjectOpenHashMap<>();
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            drawGroups.put(layer, new ArrayList<>());
        }
        List<DynamicGpuData.ChunkSectionInfo> sectionInfos = new ArrayList<>();
        GpuTextureView blockAtlas = minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        int atlasWidth = blockAtlas.getWidth(0);
        int atlasHeight = blockAtlas.getHeight(0);
        int largestIndexCount = 0;
        SectionRenderDispatcher dispatcher = minecraft.levelRenderer.sectionRenderDispatcher();
        if (dispatcher != null) {
            dispatcher.lock();
            try {
                dispatcher.uploadTerrainBuffersToGpu();
                long now = Util.getMillis();
                long fadeDuration = fadeDuration(minecraft);
                for (SectionRenderDispatcher.RenderSection section : this.sections) {
                    SectionMesh mesh = section.getSectionMesh();
                    BlockPos renderOffset = section.getRenderOrigin();
                    int sectionIndex = -1;
                    for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                        SectionMesh.SectionDraw draw = mesh.getSectionDraw(layer);
                        SectionRenderDispatcher.RenderSectionBufferSlice slice = dispatcher.getRenderSectionSlice(mesh, layer);
                        if (slice == null || draw == null || draw.hasCustomIndexBuffer() && slice.indexBuffer() == null) {
                            continue;
                        }
                        if (sectionIndex == -1) {
                            sectionIndex = sectionInfos.size();
                            sectionInfos.add(new DynamicGpuData.ChunkSectionInfo(renderOffset.getX(), renderOffset.getY(), renderOffset.getZ(),
                                    section.getVisibility(now, fadeDuration)));
                        }
                        int combinedHash = 173 + layer.ordinal();
                        VertexFormat vertexFormat = layer.pipeline(false).getVertexFormatBinding(0);
                        GpuBuffer vertexBuffer = slice.vertexBuffer();
                        combinedHash = 31 * combinedHash + vertexBuffer.hashCode();
                        int firstIndex = 0;
                        GpuBuffer indexBuffer;
                        IndexType indexType;
                        if (!draw.hasCustomIndexBuffer()) {
                            largestIndexCount = Math.max(largestIndexCount, draw.indexCount());
                            indexBuffer = null;
                            indexType = null;
                        } else {
                            indexBuffer = slice.indexBuffer();
                            indexType = draw.indexType();
                            combinedHash = 31 * combinedHash + indexBuffer.hashCode();
                            combinedHash = 31 * combinedHash + indexType.hashCode();
                            firstIndex = (int) (slice.indexBufferOffset() / indexType.bytes);
                        }
                        int baseVertex = (int) (slice.vertexBufferOffset() / vertexFormat.getVertexSize());
                        List<DrawGroup> layerGroups = drawGroups.get(layer);
                        DrawGroup group;
                        if (layer.translucent()) {
                            group = layerGroups.isEmpty() || layerGroups.getLast().hash() != combinedHash ? null : layerGroups.getLast();
                        } else {
                            group = groupCache.get(combinedHash);
                        }
                        if (group == null) {
                            group = new DrawGroup(combinedHash, vertexBuffer, indexBuffer, indexType, new ArrayList<>());
                            layerGroups.add(group);
                            if (!layer.translucent()) {
                                groupCache.put(combinedHash, group);
                            }
                        }
                        group.draws().add(new DynamicGpuData.IndexedDraw(draw.indexCount(), 1, firstIndex, baseVertex, sectionIndex));
                    }
                }
            } finally {
                dispatcher.unlock();
            }
        }
        if (largestIndexCount != 0) {
            RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).getBuffer(largestIndexCount);
        }
        GpuBufferSlice terrainTransform = RenderSystem.getDynamicUniforms().writeTerrainTransform(viewMatrix, atlasWidth, atlasHeight);
        if (minecraft.levelRenderer.isChunkRenderingUsingMultiDrawIndirect()) {
            return buildIndirect(drawGroups, sectionInfos, terrainTransform, largestIndexCount);
        }
        return buildSeparate(drawGroups, sectionInfos, terrainTransform, largestIndexCount);
    }

    private static List<DrawGroup> orderedGroups(ChunkSectionLayer layer, EnumMap<ChunkSectionLayer, List<DrawGroup>> drawGroups) {
        return layer.translucent() ? drawGroups.get(layer).reversed() : drawGroups.get(layer);
    }

    private static List<DynamicGpuData.IndexedDraw> orderedDraws(ChunkSectionLayer layer, DrawGroup group) {
        return layer.translucent() ? group.draws().reversed() : group.draws();
    }

    private static ChunkSectionsToRender buildSeparate(EnumMap<ChunkSectionLayer, List<DrawGroup>> drawGroups,
                                                       List<DynamicGpuData.ChunkSectionInfo> sectionInfos, GpuBufferSlice terrainTransform,
                                                       int largestIndexCount) {
        EnumMap<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> draws = new EnumMap<>(ChunkSectionLayer.class);
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            List<RenderPass.Draw<GpuBufferSlice[]>> layerDraws = new ArrayList<>();
            for (DrawGroup group : orderedGroups(layer, drawGroups)) {
                for (DynamicGpuData.IndexedDraw draw : orderedDraws(layer, group)) {
                    int sectionIndex = draw.baseInstance();
                    layerDraws.add(new RenderPass.Draw<>(0, group.vertexBuffer(), group.indexBuffer(), group.indexType(), draw.firstIndex(),
                            draw.indexCount(), draw.baseVertex(),
                            (sectionUbos, uploader) -> uploader.setUniform("ChunkSection", sectionUbos[sectionIndex])));
                }
            }
            draws.put(layer, layerDraws);
        }
        GpuBufferSlice[] chunkSectionInfos = RenderSystem.getDynamicUniforms()
                .writeChunkSections(sectionInfos.toArray(new DynamicGpuData.ChunkSectionInfo[0]));
        return new ChunkSectionsToRender.DrawSeparate(terrainTransform, draws, largestIndexCount, chunkSectionInfos);
    }

    private static ChunkSectionsToRender buildIndirect(EnumMap<ChunkSectionLayer, List<DrawGroup>> drawGroups,
                                                       List<DynamicGpuData.ChunkSectionInfo> sectionInfos, GpuBufferSlice terrainTransform,
                                                       int largestIndexCount) {
        List<List<DynamicGpuData.IndexedDraw>> batchedDraws = new ArrayList<>();
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            for (DrawGroup group : orderedGroups(layer, drawGroups)) {
                batchedDraws.add(orderedDraws(layer, group));
            }
        }
        GpuBufferSlice[] commandSlices = batchedDraws.isEmpty() ? new GpuBufferSlice[0]
                : RenderSystem.getDynamicUniforms().writeChunkSectionCommands(batchedDraws);
        EnumMap<ChunkSectionLayer, List<ChunkSectionsToRender.GpuMultiDrawIndexedIndirect>> indirectDraws = new EnumMap<>(ChunkSectionLayer.class);
        int index = 0;
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            List<ChunkSectionsToRender.GpuMultiDrawIndexedIndirect> layerDraws = new ArrayList<>();
            for (DrawGroup group : orderedGroups(layer, drawGroups)) {
                layerDraws.add(new ChunkSectionsToRender.GpuMultiDrawIndexedIndirect(group.vertexBuffer().slice(),
                        group.indexBuffer() == null ? null : group.indexBuffer().slice(), group.indexType(), commandSlices[index++],
                        group.draws().size()));
            }
            indirectDraws.put(layer, layerDraws);
        }
        GpuBufferSlice chunkSectionInfos = RenderSystem.getDynamicUniforms().writeChunkSectionsInstanced(sectionInfos);
        return new ChunkSectionsToRender.DrawIndirect(terrainTransform, indirectDraws, largestIndexCount, chunkSectionInfos);
    }

    private record DrawGroup(int hash, GpuBuffer vertexBuffer, GpuBuffer indexBuffer, IndexType indexType, List<DynamicGpuData.IndexedDraw> draws) {
    }

    private static void queueDirtySections(Minecraft minecraft, ObjectArrayList<SectionRenderDispatcher.RenderSection> sections) {
        SectionUpdateTracker tracker = ((LevelExtractorPortalAccessor) minecraft.levelExtractor).anthology$getSectionUpdateTracker();
        if (tracker == null || minecraft.level == null) {
            return;
        }
        RenderRegionCache cache = null;
        int queued = 0;
        for (SectionRenderDispatcher.RenderSection section : sections) {
            if (queued >= MAX_REBUILDS_PER_SCENE) {
                break;
            }
            long sectionNode = section.getSectionNode();
            SectionUpdateTracker.SectionDirtyState dirtyState = tracker.getDirtyState(sectionNode);
            if (dirtyState == null || !dirtyState.isDirty()
                    || section.getSectionMesh() == CompiledSectionMesh.UNCOMPILED && !tracker.hasAllNeighbors(minecraft.level, sectionNode)) {
                continue;
            }
            if (cache == null) {
                cache = new RenderRegionCache();
            }
            section.compileAsync(cache.createRegion(minecraft.level, sectionNode));
            dirtyState.setNotDirty();
            queued++;
        }
    }
}
