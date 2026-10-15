package com.craisinlord.anthology.portal.client;

import com.craisinlord.anthology.portal.client.PortalRenderState;
import com.craisinlord.anthology.mixins.portal.client.LevelExtractorPortalAccessor;
import com.craisinlord.anthology.mixins.portal.client.LevelRendererPortalViewAreaAccessor;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
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
import net.minecraft.client.renderer.DynamicUniforms;
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
import org.joml.Matrix4f;
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
    public void drawGroup(Minecraft minecraft, ChunkSectionLayerGroup group) {
        if (this.chunkSections != null) {
            this.chunkSections.renderGroup(group, sampler(minecraft));
        }
    }

    @Override
    public void forEachBlockEntity(Minecraft minecraft, Frustum frustum, BiConsumer<BlockEntity, Boolean> consumer) {
        long now = Util.getMillis();
        for (SectionRenderDispatcher.RenderSection section : this.sections) {
            List<BlockEntity> blockEntities = section.getSectionMesh().getRenderableBlockEntities();
            if (blockEntities.isEmpty() || section.getVisibility(now) < MIN_VISIBILITY) {
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

    private ChunkSectionsToRender buildDraws(Minecraft minecraft, Matrix4fc viewMatrix) {
        EnumMap<ChunkSectionLayer, Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>> drawGroups = new EnumMap<>(ChunkSectionLayer.class);
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            drawGroups.put(layer, new Int2ObjectOpenHashMap<>());
        }
        List<DynamicUniforms.ChunkSectionInfo> sectionInfos = new ArrayList<>();
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
                for (SectionRenderDispatcher.RenderSection section : this.sections) {
                    SectionMesh mesh = section.getSectionMesh();
                    BlockPos renderOffset = section.getRenderOrigin();
                    int uboIndex = -1;
                    for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                        SectionMesh.SectionDraw draw = mesh.getSectionDraw(layer);
                        SectionRenderDispatcher.RenderSectionBufferSlice slice = dispatcher.getRenderSectionSlice(mesh, layer);
                        if (slice == null || draw == null || draw.hasCustomIndexBuffer() && slice.indexBuffer() == null) {
                            continue;
                        }
                        if (uboIndex == -1) {
                            uboIndex = sectionInfos.size();
                            sectionInfos.add(new DynamicUniforms.ChunkSectionInfo(new Matrix4f(viewMatrix), renderOffset.getX(), renderOffset.getY(),
                                    renderOffset.getZ(), section.getVisibility(now), atlasWidth, atlasHeight));
                        }
                        int combinedHash = 173;
                        VertexFormat vertexFormat = layer.pipeline().getVertexFormatBinding(0);
                        GpuBuffer vertexBuffer = slice.vertexBuffer();
                        if (layer != ChunkSectionLayer.TRANSLUCENT) {
                            combinedHash = 31 * combinedHash + vertexBuffer.hashCode();
                        }
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
                            if (layer != ChunkSectionLayer.TRANSLUCENT) {
                                combinedHash = 31 * combinedHash + indexBuffer.hashCode();
                                combinedHash = 31 * combinedHash + indexType.hashCode();
                            }
                            firstIndex = (int) (slice.indexBufferOffset() / indexType.bytes);
                        }
                        int sectionUbo = uboIndex;
                        int baseVertex = (int) (slice.vertexBufferOffset() / vertexFormat.getVertexSize());
                        drawGroups.get(layer).computeIfAbsent(combinedHash, ignored -> new ArrayList<>()).add(new RenderPass.Draw<>(0, vertexBuffer,
                                indexBuffer, indexType, firstIndex, draw.indexCount(), baseVertex,
                                (sectionUbos, uploader) -> uploader.upload("ChunkSection", sectionUbos[sectionUbo])));
                    }
                }
            } finally {
                dispatcher.unlock();
            }
        }
        GpuBufferSlice[] chunkSectionInfos = RenderSystem.getDynamicUniforms()
                .writeChunkSections(sectionInfos.toArray(new DynamicUniforms.ChunkSectionInfo[0]));
        return new ChunkSectionsToRender(blockAtlas, drawGroups, largestIndexCount, chunkSectionInfos);
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
