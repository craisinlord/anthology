package com.craisinlord.anthology.portal.client;

import com.craisinlord.anthology.portal.PortalEntity;
import com.craisinlord.anthology.portal.PortalHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class PortalEntityRenderer<T extends PortalEntity> extends EntityRenderer<T, PortalEntityRenderer.State> {
    public PortalEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    protected boolean affectedByCulling(T entity) {
        return false;
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        boolean fancy = isFancyPortalsEnabled();
        int frames = Math.max(1, entity.appearance().surfaceFrames());
        float frameHeight = 1.0F / frames;
        state.widthDirection = entity.getWidthVec().normalize();
        state.upDirection = entity.getUpVec().normalize();
        state.normalDirection = entity.getNormalVec().normalize();
        state.normal = entity.getNormalVec();
        state.scale = entity.getPortalVisualScale(partialTick);
        state.halfWidth = entity.getPortalWidth() * 0.5F;
        state.halfHeight = entity.getPortalHeight() * 0.5F;
        state.color = entity.appearance().color();
        state.fancy = fancy;
        state.frameHeight = frameHeight;
        state.frameV = Math.floorMod((int) ((entity.level().getGameTime() + partialTick) / 5.0F), frames) * frameHeight;
        state.texture = entity.appearance().surfaceTexture(fancy);
        state.rimTexture = PortalRim.texture(entity);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraState) {
        if (state.texture == null) {
            return;
        }
        poseStack.pushPose();
        float halfWidth = state.halfWidth;
        float halfHeight = state.halfHeight;
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(state.texture), (pose, consumer) -> {
            Matrix4f matrix = pose.pose();
            if (state.fancy) {
                putAnimatedPortalVertex(consumer, matrix, state, -halfWidth, -halfHeight, 0.0F, 1.0F);
                putAnimatedPortalVertex(consumer, matrix, state, -halfWidth, halfHeight, 0.0F, 0.0F);
                putAnimatedPortalVertex(consumer, matrix, state, halfWidth, halfHeight, 1.0F, 0.0F);
                putAnimatedPortalVertex(consumer, matrix, state, halfWidth, -halfHeight, 1.0F, 1.0F);
            } else {
                putPortalVertex(consumer, matrix, state, -halfWidth, -halfHeight, 0.0F, 1.0F, true);
                putPortalVertex(consumer, matrix, state, -halfWidth, halfHeight, 0.0F, 0.0F, true);
                putPortalVertex(consumer, matrix, state, halfWidth, halfHeight, 1.0F, 0.0F, true);
                putPortalVertex(consumer, matrix, state, halfWidth, -halfHeight, 1.0F, 1.0F, true);
            }
        });
        float rimWidth = halfWidth + PortalRim.EXTRA_WIDTH;
        float rimHeight = halfHeight + PortalRim.EXTRA_HEIGHT;
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(state.rimTexture), (pose, edge) -> {
            Matrix4f matrix = pose.pose();
            putPortalVertex(edge, matrix, state, -rimWidth, -rimHeight, 0.0F, 1.0F, true);
            putPortalVertex(edge, matrix, state, -rimWidth, rimHeight, 0.0F, 0.0F, true);
            putPortalVertex(edge, matrix, state, rimWidth, rimHeight, 1.0F, 0.0F, true);
            putPortalVertex(edge, matrix, state, rimWidth, -rimHeight, 1.0F, 1.0F, true);
        });
        poseStack.popPose();
    }

    private static boolean isFancyPortalsEnabled() {
        Minecraft minecraft = Minecraft.getInstance();
        GraphicsPreset preset = minecraft.options.graphicsPreset().get();
        return PortalHooks.settings().fancySurfaces()
                && preset != GraphicsPreset.FAST
                && !minecraft.options.improvedTransparency().get();
    }

    private static Vec3 vertexPosition(State state, float horizontal, float vertical) {
        return state.widthDirection.scale(horizontal * state.scale)
                .add(state.upDirection.scale(vertical * state.scale))
                .add(state.normalDirection.scale(0.03125D));
    }

    private static void putAnimatedPortalVertex(VertexConsumer consumer, Matrix4f pose, State state, float horizontal, float vertical, float u, float v) {
        Vec3 position = vertexPosition(state, horizontal, vertical);
        consumer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .setColor(255, 255, 255, 210)
                .setUv(u, state.frameV + v * state.frameHeight)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal((float) state.normal.x, (float) state.normal.y, (float) state.normal.z);
    }

    private static void putPortalVertex(VertexConsumer consumer, Matrix4f pose, State state, float horizontal, float vertical, float u, float v, boolean channelTint) {
        Vec3 position = vertexPosition(state, horizontal, vertical);
        int color = state.color;
        int red = channelTint ? color >> 16 & 0xFF : 255;
        int green = channelTint ? color >> 8 & 0xFF : 255;
        int blue = channelTint ? color & 0xFF : 255;
        int alpha = channelTint ? PortalRim.ENTITY_ALPHA : 210;
        consumer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal((float) state.normal.x, (float) state.normal.y, (float) state.normal.z);
    }

    public static class State extends EntityRenderState {
        public Vec3 widthDirection = Vec3.ZERO;
        public Vec3 upDirection = Vec3.ZERO;
        public Vec3 normalDirection = Vec3.ZERO;
        public Vec3 normal = Vec3.ZERO;
        public float scale;
        public float halfWidth;
        public float halfHeight;
        public int color;
        public boolean fancy;
        public float frameV;
        public float frameHeight = 1.0F;
        public Identifier texture;
        public Identifier rimTexture;
    }
}
