package com.craisinlord.anthology.mixins.portal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelRenderer.class)
public interface LevelRendererPortalSceneInvoker {
    @Invoker("renderEntity")
    void anthology$renderPortalEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource);

    @Invoker("renderSnowAndRain")
    void anthology$renderPortalWeather(LightTexture lightTexture, float partialTick, double cameraX, double cameraY, double cameraZ);

    @Invoker("renderWorldBorder")
    void anthology$renderPortalWorldBorder(Camera camera);
}
