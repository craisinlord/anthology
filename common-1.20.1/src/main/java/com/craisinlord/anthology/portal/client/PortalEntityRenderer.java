package com.craisinlord.anthology.portal.client;

import com.craisinlord.anthology.portal.PortalEntity;
import com.craisinlord.anthology.portal.PortalHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class PortalEntityRenderer<T extends PortalEntity> extends EntityRenderer<T> {
    public PortalEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        Matrix4f pose = poseStack.last().pose();
        float portalScale = entity.getPortalVisualScale(partialTick);
        float halfWidth = entity.getPortalWidth() * 0.5F;
        float halfHeight = entity.getPortalHeight() * 0.5F;
        ResourceLocation texture = portalTexture(entity);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));
        if (isFancyPortalsEnabled()) {
            int frames = Math.max(1, entity.appearance().surfaceFrames());
            float frameHeight = 1.0F / frames;
            float frameV = Math.floorMod((int) ((entity.level().getGameTime() + partialTick) / 5.0F), frames) * frameHeight;
            putAnimatedPortalVertex(consumer, pose, entity, portalScale, frameV, frameHeight, -halfWidth, -halfHeight, 0.0F, 1.0F);
            putAnimatedPortalVertex(consumer, pose, entity, portalScale, frameV, frameHeight, -halfWidth, halfHeight, 0.0F, 0.0F);
            putAnimatedPortalVertex(consumer, pose, entity, portalScale, frameV, frameHeight, halfWidth, halfHeight, 1.0F, 0.0F);
            putAnimatedPortalVertex(consumer, pose, entity, portalScale, frameV, frameHeight, halfWidth, -halfHeight, 1.0F, 1.0F);
        } else {
            putPortalVertex(consumer, pose, entity, portalScale, -halfWidth, -halfHeight, 0.0F, 1.0F, true);
            putPortalVertex(consumer, pose, entity, portalScale, -halfWidth, halfHeight, 0.0F, 0.0F, true);
            putPortalVertex(consumer, pose, entity, portalScale, halfWidth, halfHeight, 1.0F, 0.0F, true);
            putPortalVertex(consumer, pose, entity, portalScale, halfWidth, -halfHeight, 1.0F, 1.0F, true);
        }
        VertexConsumer edge = bufferSource.getBuffer(RenderType.entityTranslucent(PortalRim.texture(entity)));
        float rimWidth = halfWidth + PortalRim.EXTRA_WIDTH;
        float rimHeight = halfHeight + PortalRim.EXTRA_HEIGHT;
        putPortalVertex(edge, pose, entity, portalScale, -rimWidth, -rimHeight, 0.0F, 1.0F, true);
        putPortalVertex(edge, pose, entity, portalScale, -rimWidth, rimHeight, 0.0F, 0.0F, true);
        putPortalVertex(edge, pose, entity, portalScale, rimWidth, rimHeight, 1.0F, 0.0F, true);
        putPortalVertex(edge, pose, entity, portalScale, rimWidth, -rimHeight, 1.0F, 1.0F, true);
        poseStack.popPose();
    }

    private static ResourceLocation portalTexture(PortalEntity entity) {
        return entity.appearance().surfaceTexture(isFancyPortalsEnabled());
    }

    private static boolean isFancyPortalsEnabled() {
        Minecraft minecraft = Minecraft.getInstance();
        return PortalHooks.settings().fancySurfaces()
                && minecraft.options.graphicsMode().get() == GraphicsStatus.FANCY;
    }

    private static void putAnimatedPortalVertex(VertexConsumer consumer, Matrix4f pose, PortalEntity entity, float portalScale, float frameV, float frameHeight, float horizontal, float vertical, float u, float v) {
        Vec3 position = entity.getWidthVec().normalize().scale(horizontal * portalScale)
                .add(entity.getUpVec().normalize().scale(vertical * portalScale))
                .add(entity.getNormalVec().normalize().scale(0.03125D));
        consumer.vertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .color(255, 255, 255, 210)
                .uv(u, frameV + v * frameHeight)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal((float) entity.getNormalVec().x, (float) entity.getNormalVec().y, (float) entity.getNormalVec().z)
                .endVertex();
    }

    private static void putPortalVertex(VertexConsumer consumer, Matrix4f pose, PortalEntity entity, float portalScale, float horizontal, float vertical, float u, float v, boolean channelTint) {
        Vec3 position = entity.getWidthVec().normalize().scale(horizontal * portalScale)
                .add(entity.getUpVec().normalize().scale(vertical * portalScale))
                .add(entity.getNormalVec().normalize().scale(0.03125D));
        int color = entity.appearance().color();
        int red = channelTint ? color >> 16 & 0xFF : 255;
        int green = channelTint ? color >> 8 & 0xFF : 255;
        int blue = channelTint ? color & 0xFF : 255;
        int alpha = channelTint ? PortalRim.ENTITY_ALPHA : 210;
        consumer.vertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal((float) entity.getNormalVec().x, (float) entity.getNormalVec().y, (float) entity.getNormalVec().z)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return portalTexture(entity);
    }
}
