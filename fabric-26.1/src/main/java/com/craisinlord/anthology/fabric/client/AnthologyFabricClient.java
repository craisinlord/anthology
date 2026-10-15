package com.craisinlord.anthology.fabric.client;

import com.craisinlord.anthology.fabric.AnthologyFabricClientBridge;
import com.craisinlord.anthology.fabric.AnthologyFabricEntities;
import com.craisinlord.anthology.network.AnthologyNetwork;
import com.craisinlord.anthology.gravity.GravityHooks;
import com.craisinlord.anthology.portal.client.PortalRollClientState;
import com.craisinlord.anthology.registry.AnthologyEntities;
import com.craisinlord.anthology.shake.client.CameraShake;
import com.craisinlord.anthology.time.client.ClientTimeDilationTicker;
import com.craisinlord.anthology.time.client.TimeDilationFieldRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class AnthologyFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AnthologyFabricClientBridge.setServerSender(ClientPlayNetworking::send);
        EntityRendererRegistry.register(AnthologyEntities.timeDilationField(), TimeDilationFieldRenderer::new);
        GravityHooks.addCameraRollSource(PortalRollClientState::getRollDegrees);
        EntityRendererRegistry.register(AnthologyFabricEntities.MULTIPART_PART, MultipartPartRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null) {
                ClientTimeDilationTicker.tick(client.level);
            }
            CameraShake.tick();
            PortalRollClientState.tick(client);
        });
        AnthologyNetwork.onClientbound(AnthologyFabricClient::registerClientboundReceiver);
    }

    private static <T extends CustomPacketPayload> void registerClientboundReceiver(AnthologyNetwork.ClientboundPayload<T> payload) {
        ClientPlayNetworking.registerGlobalReceiver(payload.type(), (message, context) ->
                context.client().execute(() -> payload.handler().accept(message, context.player())));
    }
}
