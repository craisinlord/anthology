package com.craisinlord.anthology.portal.client;

import com.craisinlord.anthology.Anthology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class PortalViewPipelines {
    public static final RenderPipeline PORTAL_VIEW = RenderPipeline.builder()
            .withLocation(id("pipeline/portal_view"))
            .withVertexShader(id("core/portal_view"))
            .withFragmentShader(id("core/portal_view"))
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLES)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
            .withCull(false)
            .build();
    public static final RenderPipeline PORTAL_QUERY = RenderPipeline.builder()
            .withLocation(id("pipeline/portal_query"))
            .withVertexShader(id("core/portal_clip"))
            .withFragmentShader(id("core/portal_clip"))
            .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.TRIANGLES)
            .withColorTargetState(new ColorTargetState(Optional.empty(), ColorTargetState.WRITE_NONE))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();
    public static final RenderPipeline RIM_OVERLAY = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(id("pipeline/portal_rim_overlay"))
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withCull(false)
            .build();
    public static final RenderPipeline RIM_HIDDEN = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(id("pipeline/portal_rim_hidden"))
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN, false))
            .withCull(false)
            .build();
    public static final RenderPipeline MOON_COLOR = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(id("pipeline/portal_moon_color"))
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withCull(false)
            .build();
    public static final RenderPipeline MOON_TEXTURE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(id("pipeline/portal_moon_texture"))
            .withVertexShader("core/position_tex")
            .withFragmentShader("core/position_tex")
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.OVERLAY))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
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
