package com.craisinlord.anthology.portal.client;

import com.craisinlord.anthology.Anthology;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;

public final class PortalViewPipelines {
    private static final RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET = RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .buildSnippet();
    public static final RenderPipeline PORTAL_VIEW = RenderPipeline.builder()
            .withLocation(id("pipeline/portal_view"))
            .withVertexShader(id("core/portal_view"))
            .withFragmentShader(id("core/portal_view"))
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
            .withCull(false)
            .build();
    public static final RenderPipeline PORTAL_QUERY = RenderPipeline.builder()
            .withLocation(id("pipeline/portal_query"))
            .withVertexShader(id("core/portal_clip"))
            .withFragmentShader(id("core/portal_clip"))
            .withVertexBinding(0, DefaultVertexFormat.POSITION)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_NONE))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();
    public static final RenderPipeline RIM_OVERLAY = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withLocation(id("pipeline/portal_rim_overlay"))
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .build();
    public static final RenderPipeline RIM_HIDDEN = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withLocation(id("pipeline/portal_rim_hidden"))
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN, false))
            .withCull(false)
            .build();
    public static final RenderPipeline MOON_COLOR = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withLocation(id("pipeline/portal_moon_color"))
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .build();
    public static final RenderPipeline MOON_TEXTURE = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withLocation(id("pipeline/portal_moon_texture"))
            .withVertexShader("core/position_tex")
            .withFragmentShader("core/position_tex")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.OVERLAY))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .build();

    private PortalViewPipelines() {
    }

    public static List<RenderPipeline> all() {
        return List.of(PORTAL_VIEW, PORTAL_QUERY, RIM_OVERLAY, RIM_HIDDEN, MOON_COLOR, MOON_TEXTURE);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Anthology.MODID, path);
    }
}
