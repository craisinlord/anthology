package com.craisinlord.anthology.beam.client;

import com.craisinlord.anthology.Anthology;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.function.Function;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public final class BeamRenderTypes {
    public static final RenderPipeline BEAM_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Anthology.id("pipeline/beam"))
            .withVertexShader("core/entity")
            .withFragmentShader("core/entity")
            .withShaderDefine("EMISSIVE")
            .withShaderDefine("NO_OVERLAY")
            .withShaderDefine("NO_CARDINAL_LIGHTING")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .build();

    public static final OitPipelineSet BEAM_OIT_PIPELINES = OitPipelineSet.builder("anthology_beam",
                    RenderPipeline.builder(RenderPipelines.OIT_ENTITY_SNIPPET)
                            .withShaderDefine("EMISSIVE")
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withCull(false))
            .build();

    private static final Function<Identifier, RenderType> BEAM = Util.memoize(texture -> RenderType.create(
            "anthology_beam",
            RenderSetup.builder(BEAM_PIPELINE)
                    .setOitPipelines(BEAM_OIT_PIPELINES)
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .affectsCrumbling()
                    .sortOnUpload()
                    .createRenderSetup()));

    private BeamRenderTypes() {
    }

    public static RenderType beam(Identifier texture) {
        return BEAM.apply(texture);
    }
}
