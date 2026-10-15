package com.craisinlord.anthology.fabric.client;

import com.craisinlord.anthology.Anthology;
import com.craisinlord.anthology.fabric.AnthologyFabric;
import com.craisinlord.anthology.fabric.AnthologyFabricClientBridge;
import com.craisinlord.anthology.fabric.AnthologyFabricEntities;
import com.craisinlord.anthology.gravity.GravityHooks;
import com.craisinlord.anthology.network.AnthologyNetwork;
import com.craisinlord.anthology.network.CustomPacketPayload;
import com.craisinlord.anthology.portal.client.PortalRollClientState;
import com.craisinlord.anthology.portal.client.PortalViewRenderer;
import com.craisinlord.anthology.registry.AnthologyEntities;
import com.craisinlord.anthology.shake.client.CameraShake;
import com.craisinlord.anthology.time.client.ClientTimeDilationTicker;
import com.craisinlord.anthology.time.client.TimeDilationFieldRenderer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public final class AnthologyFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AnthologyFabricClientBridge.setServerSender(payload ->
                ClientPlayNetworking.send(payload.type().id(), AnthologyFabric.encode(payload)));
        EntityRendererRegistry.register(AnthologyEntities.timeDilationField(), TimeDilationFieldRenderer::new);
        EntityRendererRegistry.register(AnthologyFabricEntities.MULTIPART_PART, MultipartPartRenderer::new);
        GravityHooks.addCameraRollSource(PortalRollClientState::getRollDegrees);
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            PortalViewRenderer.clearPortalViewShader();
            try {
                context.register(Anthology.id("portal_view"), DefaultVertexFormat.POSITION_TEX, PortalViewRenderer::setPortalViewShader);
            } catch (java.io.IOException | RuntimeException exception) {
                Anthology.LOGGER.error("Failed to register portal view shader", exception);
            }
        });
        WorldRenderEvents.LAST.register(context -> PortalViewRenderer.render(context.camera(), context.matrixStack().last().pose(),
                context.projectionMatrix(), context.tickDelta()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null) {
                ClientTimeDilationTicker.tick(client.level);
            }
            CameraShake.tick();
            PortalRollClientState.tick(client);
        });
        AnthologyNetwork.onClientbound(AnthologyFabricClient::registerClientboundReceiver);
    }

    private static void registerClientboundReceiver(AnthologyNetwork.ClientboundPayload<?> payload) {
        ClientPlayNetworking.registerGlobalReceiver(payload.type().id(), (client, handler, buf, responseSender) -> {
            CustomPacketPayload message = payload.decode(buf);
            client.execute(() -> payload.handle(message, client.player));
        });
    }
}
