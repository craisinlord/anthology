package com.craisinlord.anthology.fabric.client;

import com.craisinlord.anthology.fabric.AnthologyFabricClientBridge;
import com.craisinlord.anthology.fabric.AnthologyFabricEntities;
import com.craisinlord.anthology.network.AnthologyNetwork;
import com.craisinlord.anthology.Anthology;
import com.craisinlord.anthology.gravity.GravityHooks;
import com.craisinlord.anthology.portal.client.PortalRollClientState;
import com.craisinlord.anthology.portal.client.PortalViewRenderer;
import com.craisinlord.anthology.registry.AnthologyEntities;
import com.craisinlord.anthology.shake.client.CameraShake;
import com.craisinlord.anthology.time.client.ClientTimeDilationTicker;
import com.craisinlord.anthology.time.client.TimeDilationFieldRenderer;
import net.fabricmc.api.ClientModInitializer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class AnthologyFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AnthologyFabricClientBridge.setServerSender(ClientPlayNetworking::send);
        EntityRendererRegistry.register(AnthologyEntities.timeDilationField(), TimeDilationFieldRenderer::new);
        GravityHooks.addCameraRollSource(PortalRollClientState::getRollDegrees);
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            PortalViewRenderer.clearPortalViewShader();
            try {
                context.register(Anthology.id("portal_view"), DefaultVertexFormat.POSITION_TEX, PortalViewRenderer::setPortalViewShader);
            } catch (java.io.IOException | RuntimeException exception) {
                Anthology.LOGGER.error("Failed to register portal view shader", exception);
            }
        });
        WorldRenderEvents.LAST.register(context -> PortalViewRenderer.render(context.camera(), context.positionMatrix(), context.projectionMatrix(), context.tickCounter()));
        EntityRendererRegistry.register(AnthologyFabricEntities.MULTIPART_PART, MultipartPartRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null) {
                ClientTimeDilationTicker.tick(client.level);
            }
            CameraShake.tick();
            PortalRollClientState.tick(client);
        });
        for (AnthologyNetwork.ClientboundPayload<?> payload : AnthologyNetwork.clientbound()) {
            registerClientboundReceiver(payload);
        }
    }

    private static <T extends CustomPacketPayload> void registerClientboundReceiver(AnthologyNetwork.ClientboundPayload<T> payload) {
        ClientPlayNetworking.registerGlobalReceiver(payload.type(), (message, context) ->
                context.client().execute(() -> payload.handler().accept(message, context.player())));
    }
}
