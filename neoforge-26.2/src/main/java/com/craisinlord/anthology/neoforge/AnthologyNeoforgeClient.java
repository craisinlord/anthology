package com.craisinlord.anthology.neoforge;

import com.craisinlord.anthology.beam.client.BeamRenderTypes;
import com.craisinlord.anthology.gravity.GravityHooks;
import com.craisinlord.anthology.portal.client.PortalRollClientState;
import com.craisinlord.anthology.portal.client.PortalViewRenderer;
import com.craisinlord.anthology.registry.AnthologyEntities;
import com.craisinlord.anthology.shake.client.CameraShake;
import com.craisinlord.anthology.time.client.ClientTimeDilationTicker;
import com.craisinlord.anthology.time.client.TimeDilationFieldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

final class AnthologyNeoforgeClient {
    private AnthologyNeoforgeClient() {
    }

    static void init(IEventBus modEventBus) {
        GravityHooks.addCameraRollSource(PortalRollClientState::getRollDegrees);
        modEventBus.addListener(AnthologyNeoforgeClient::registerRenderers);
        modEventBus.addListener(AnthologyNeoforgeClient::registerRenderPipelines);
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

    private static void registerRenderPipelines(RegisterRenderPipelinesEvent event) {
        PortalViewRenderer.pipelines().forEach(event::registerPipeline);
        event.registerPipeline(BeamRenderTypes.BEAM_PIPELINE);
    }

    static void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    private static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            CameraShake.clear();
            return;
        }
        CameraShake.Angles angles = CameraShake.angles(event.getCamera().position(), (float) (minecraft.player.tickCount + event.getPartialTick()));
        if (angles != null) {
            event.setYaw(event.getYaw() + angles.yaw());
            event.setPitch(event.getPitch() + angles.pitch());
            event.setRoll(event.getRoll() + angles.roll());
        }
    }
}
