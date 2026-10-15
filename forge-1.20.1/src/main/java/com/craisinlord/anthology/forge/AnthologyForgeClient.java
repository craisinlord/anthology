package com.craisinlord.anthology.forge;

import com.craisinlord.anthology.Anthology;
import com.craisinlord.anthology.gravity.GravityHooks;
import com.craisinlord.anthology.portal.client.PortalRollClientState;
import com.craisinlord.anthology.portal.client.PortalViewRenderer;
import com.craisinlord.anthology.registry.AnthologyEntities;
import com.craisinlord.anthology.shake.client.CameraShake;
import com.craisinlord.anthology.time.client.ClientTimeDilationTicker;
import com.craisinlord.anthology.time.client.TimeDilationFieldRenderer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

final class AnthologyForgeClient {
    private static Matrix4f levelViewMatrix;

    private AnthologyForgeClient() {
    }

    static void init(IEventBus modEventBus) {
        GravityHooks.addCameraRollSource(PortalRollClientState::getRollDegrees);
        modEventBus.addListener(AnthologyForgeClient::registerRenderers);
        modEventBus.addListener(AnthologyForgeClient::registerShaders);
        MinecraftForge.EVENT_BUS.addListener(AnthologyForgeClient::renderLevelStage);
        MinecraftForge.EVENT_BUS.addListener(AnthologyForgeClient::clientTick);
        MinecraftForge.EVENT_BUS.addListener(AnthologyForgeClient::cameraAngles);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AnthologyEntities.timeDilationField(), TimeDilationFieldRenderer::new);
    }

    private static void registerShaders(RegisterShadersEvent event) {
        PortalViewRenderer.clearPortalViewShader();
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), Anthology.id("portal_view"), DefaultVertexFormat.POSITION_TEX),
                    PortalViewRenderer::setPortalViewShader);
        } catch (java.io.IOException | RuntimeException exception) {
            Anthology.LOGGER.error("Failed to register portal view shader", exception);
        }
    }

    private static void renderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            levelViewMatrix = new Matrix4f(event.getPoseStack().last().pose());
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            Matrix4f viewMatrix = levelViewMatrix;
            levelViewMatrix = null;
            if (viewMatrix == null) {
                viewMatrix = new Matrix4f().rotation(event.getCamera().rotation().conjugate(new Quaternionf()));
            }
            PortalViewRenderer.render(event.getCamera(), viewMatrix, event.getProjectionMatrix(), event.getPartialTick());
        }
    }

    private static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            ClientTimeDilationTicker.tick(minecraft.level);
        }
        CameraShake.tick();
        PortalRollClientState.tick(minecraft);
    }

    private static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            CameraShake.clear();
            return;
        }
        CameraShake.Angles angles = CameraShake.angles(event.getCamera().getPosition(), (float) (minecraft.player.tickCount + event.getPartialTick()));
        if (angles != null) {
            event.setYaw(event.getYaw() + angles.yaw());
            event.setPitch(event.getPitch() + angles.pitch());
            event.setRoll(event.getRoll() + angles.roll());
        }
    }
}
