package com.craisinlord.anthology.portal.client;

import com.craisinlord.anthology.mixins.portal.client.LevelRendererPortalViewAreaAccessor;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class VanillaPortalTerrain implements PortalTerrain {
    private static final int MAX_REBUILDS_PER_SCENE = 32;

    private final ObjectArrayList<ChunkRenderDispatcher.RenderChunk> sections;

    private VanillaPortalTerrain(ObjectArrayList<ChunkRenderDispatcher.RenderChunk> sections) {
        this.sections = sections;
    }

    static VanillaPortalTerrain prepare(Minecraft minecraft, UUID exitId, Vec3 exitCenter, Vec3 exitNormal, Frustum frustum) {
        PortalSectionViews.View view = PortalSectionViews.prepare(minecraft, exitId, exitCenter, exitNormal, frustum);
        view.visibleSections().removeIf(section -> !PortalRenderState.shouldRenderBounds(section.getBoundingBox()));
        queueDirtySections(minecraft, view.visibleSections());
        return new VanillaPortalTerrain(view.visibleSections());
    }

    @Override
    public int sectionCount() {
        return this.sections.size();
    }

    @Override
    public void begin(Minecraft minecraft) {
    }

    @Override
    public void end(Minecraft minecraft) {
    }

    @Override
    public void drawLayer(Minecraft minecraft, RenderType renderType, Vec3 cameraPosition, Matrix4f viewMatrix, Matrix4f projection) {
        renderType.setupRenderState();
        ShaderInstance shader = RenderSystem.getShader();
        if (shader == null) {
            renderType.clearRenderState();
            return;
        }
        for (int sampler = 0; sampler < 12; sampler++) {
            shader.setSampler("Sampler" + sampler, RenderSystem.getShaderTexture(sampler));
        }
        if (shader.MODEL_VIEW_MATRIX != null) {
            shader.MODEL_VIEW_MATRIX.set(viewMatrix);
        }
        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(projection);
        }
        if (shader.COLOR_MODULATOR != null) {
            shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        }
        if (shader.GLINT_ALPHA != null) {
            shader.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
        }
        if (shader.FOG_START != null) {
            shader.FOG_START.set(RenderSystem.getShaderFogStart());
        }
        if (shader.FOG_END != null) {
            shader.FOG_END.set(RenderSystem.getShaderFogEnd());
        }
        if (shader.FOG_COLOR != null) {
            shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        }
        if (shader.FOG_SHAPE != null) {
            shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
        }
        if (shader.TEXTURE_MATRIX != null) {
            shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        }
        if (shader.GAME_TIME != null) {
            shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
        }
        RenderSystem.setupShaderLights(shader);
        shader.apply();
        Uniform chunkOffset = shader.CHUNK_OFFSET;
        boolean frontToBack = renderType != RenderType.translucent();
        ObjectListIterator<ChunkRenderDispatcher.RenderChunk> iterator =
                this.sections.listIterator(frontToBack ? 0 : this.sections.size());
        try {
            while (frontToBack ? iterator.hasNext() : iterator.hasPrevious()) {
                ChunkRenderDispatcher.RenderChunk section = frontToBack ? iterator.next() : iterator.previous();
                if (section.getCompiledChunk().isEmpty(renderType)) {
                    continue;
                }
                VertexBuffer buffer = section.getBuffer(renderType);
                BlockPos origin = section.getOrigin();
                if (chunkOffset != null) {
                    chunkOffset.set((float) (origin.getX() - cameraPosition.x), (float) (origin.getY() - cameraPosition.y),
                            (float) (origin.getZ() - cameraPosition.z));
                    chunkOffset.upload();
                }
                buffer.bind();
                buffer.draw();
            }
        } finally {
            if (chunkOffset != null) {
                chunkOffset.set(0.0F, 0.0F, 0.0F);
            }
            shader.clear();
            VertexBuffer.unbind();
            renderType.clearRenderState();
        }
    }

    @Override
    public void forEachBlockEntity(Minecraft minecraft, Frustum frustum, Consumer<BlockEntity> consumer) {
        for (ChunkRenderDispatcher.RenderChunk section : this.sections) {
            for (BlockEntity blockEntity : section.getCompiledChunk().getRenderableBlockEntities()) {
                consumer.accept(blockEntity);
            }
        }
        LevelRendererPortalViewAreaAccessor state = (LevelRendererPortalViewAreaAccessor) minecraft.levelRenderer;
        synchronized (state.anthology$getGlobalBlockEntities()) {
            for (BlockEntity blockEntity : state.anthology$getGlobalBlockEntities()) {
                if (frustum.isVisible(new AABB(blockEntity.getBlockPos()).inflate(1.0D))) {
                    consumer.accept(blockEntity);
                }
            }
        }
    }

    private static void queueDirtySections(Minecraft minecraft, ObjectArrayList<ChunkRenderDispatcher.RenderChunk> sections) {
        ChunkRenderDispatcher dispatcher = minecraft.levelRenderer.getChunkRenderDispatcher();
        if (dispatcher == null) {
            return;
        }
        RenderRegionCache cache = null;
        int queued = 0;
        for (ChunkRenderDispatcher.RenderChunk section : sections) {
            if (queued >= MAX_REBUILDS_PER_SCENE) {
                break;
            }
            if (!section.isDirty() || !minecraft.level.getLightEngine().lightOnInSection(SectionPos.of(section.getOrigin()))) {
                continue;
            }
            if (cache == null) {
                cache = new RenderRegionCache();
            }
            section.rebuildChunkAsync(dispatcher, cache);
            section.setNotDirty();
            queued++;
        }
    }
}
