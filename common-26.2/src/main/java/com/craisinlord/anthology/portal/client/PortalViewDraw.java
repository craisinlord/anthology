package com.craisinlord.anthology.portal.client;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Optional;
import java.util.OptionalDouble;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class PortalViewDraw {
    private static final int INITIAL_BUFFER_SIZE = 786432;
    private static final long VERTEX_ALIGNMENT = 16L;
    private static ByteBufferBuilder builder;

    private PortalViewDraw() {
    }

    public static BufferBuilder begin(PrimitiveTopology topology, VertexFormat format) {
        if (builder == null) {
            builder = new ByteBufferBuilder(INITIAL_BUFFER_SIZE);
        }
        return new BufferBuilder(builder, topology, format);
    }

    static void drawClip(RenderTarget target, RenderPipeline pipeline, MeshData mesh, GpuTextureView texture, GpuSampler sampler) {
        draw(target, pipeline, mesh, null, null, texture, sampler, null);
    }

    static void drawClip(RenderTarget target, RenderPipeline pipeline, MeshData mesh, Runnable beforeDraw, Runnable afterDraw) {
        draw(target, pipeline, mesh, null, null, null, null, new Runnable[] {beforeDraw, afterDraw});
    }

    public static void draw(RenderTarget target, RenderPipeline pipeline, MeshData mesh, Matrix4fc modelView, GpuBufferSlice projection,
                     GpuTextureView texture, GpuSampler sampler) {
        draw(target, pipeline, mesh, modelView, projection, texture, sampler, null);
    }

    private static void draw(RenderTarget target, RenderPipeline pipeline, MeshData mesh, Matrix4fc modelView, GpuBufferSlice projection,
                             GpuTextureView texture, GpuSampler sampler, Runnable[] hooks) {
        if (mesh == null) {
            return;
        }
        try {
            MeshData.DrawState drawState = mesh.drawState();
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            GpuBufferSlice vertices = encoder.transientMemory().uploadGpu(mesh.vertexBuffer(), VERTEX_ALIGNMENT, GpuBuffer.USAGE_VERTEX);
            RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(drawState.primitiveTopology());
            GpuBuffer indices = autoIndices.getBuffer(drawState.indexCount());
            GpuBufferSlice transforms = modelView == null ? null : RenderSystem.getDynamicUniforms()
                    .writeTransform(new Matrix4f(modelView), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
            try (RenderPass pass = encoder.createRenderPass(() -> "Portal gun portal view", target.getColorTextureView(), Optional.empty(),
                    target.useDepth ? target.getDepthTextureView() : null, OptionalDouble.empty())) {
                pass.setPipeline(pipeline);
                if (transforms != null) {
                    pass.setUniform("DynamicTransforms", transforms);
                }
                if (projection != null) {
                    pass.setUniform("Projection", projection);
                }
                if (texture != null) {
                    pass.bindTexture("Sampler0", texture, sampler);
                }
                pass.setVertexBuffer(0, vertices);
                pass.setIndexBuffer(indices, autoIndices.type());
                if (hooks != null) {
                    hooks[0].run();
                }
                try {
                    pass.drawIndexed(drawState.indexCount(), 1, 0, 0, 0);
                } finally {
                    if (hooks != null) {
                        hooks[1].run();
                    }
                }
            }
        } finally {
            mesh.close();
        }
    }
}
