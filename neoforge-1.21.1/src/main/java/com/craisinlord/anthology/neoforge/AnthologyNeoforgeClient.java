package com.craisinlord.anthology.neoforge;

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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

final class AnthologyNeoforgeClient {
    private AnthologyNeoforgeClient() {
    }

    static void init(IEventBus modEventBus) {
        GravityHooks.addCameraRollSource(PortalRollClientState::getRollDegrees);
        modEventBus.addListener(AnthologyNeoforgeClient::registerRenderers);
        modEventBus.addListener(AnthologyNeoforgeClient::registerShaders);
        NeoForge.EVENT_BUS.addListener(AnthologyNeoforgeClient::renderLevelStage);
        NeoForge.EVENT_BUS.addListener(AnthologyNeoforgeClient::clientTick);
        NeoForge.EVENT_BUS.addListener(AnthologyNeoforgeClient::cameraAngles);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AnthologyEntities.timeDilationField(), TimeDilationFieldRenderer::new);
    }

    private static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            ClientTimeDilationTicker.tick(minecraft.level);
        }
        CameraShake.tick();
        PortalRollClientState.tick(minecraft);
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
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            PortalViewRenderer.render(event.getCamera(), event.getModelViewMatrix(), event.getProjectionMatrix(), event.getPartialTick());
        }
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
