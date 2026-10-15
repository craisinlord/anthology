package com.craisinlord.anthology.fabric;

import com.craisinlord.anthology.Anthology;
import com.craisinlord.anthology.gravity.GravityCommand;
import com.craisinlord.anthology.gravity.GravitySync;
import com.craisinlord.anthology.fabric.multipart.MultipartPartEntity;
import com.craisinlord.anthology.multipart.MultipartFramework;
import com.craisinlord.anthology.network.AnthologyNetwork;
import com.craisinlord.anthology.registry.ContentBootstrap;
import com.craisinlord.anthology.time.ChronosphereManager;
import com.craisinlord.anthology.time.TimeDilationCommand;
import com.craisinlord.anthology.time.TimeDilationManager;
import com.craisinlord.anthology.time.TimeDilationSync;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class AnthologyFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Anthology.init();
        ContentBootstrap.ensure();
        MultipartFramework.setPartFactory((owner, partIndex, spec) ->
                new MultipartPartEntity(AnthologyFabricEntities.MULTIPART_PART, ((Entity) owner).level()).anthology$configure(owner, partIndex, spec));

        AnthologyNetwork.setSender(new AnthologyNetwork.Sender() {
            @Override
            public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
                ServerPlayNetworking.send(player, payload);
            }

            @Override
            public void sendToTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
                for (ServerPlayer player : PlayerLookup.tracking(entity)) {
                    if (player != entity) {
                        ServerPlayNetworking.send(player, payload);
                    }
                }
                if (entity instanceof ServerPlayer self) {
                    ServerPlayNetworking.send(self, payload);
                }
            }

            @Override
            public void sendToServer(CustomPacketPayload payload) {
                AnthologyFabricClientBridge.sendToServer(payload);
            }
        });
        AnthologyNetwork.onClientbound(AnthologyFabric::registerClientboundType);
        AnthologyNetwork.onServerbound(AnthologyFabric::registerServerbound);

        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> {
            GravityCommand.register(dispatcher);
            TimeDilationCommand.register(dispatcher);
        });
        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {
            GravitySync.syncToPlayer(player, trackedEntity);
            TimeDilationSync.syncRateToPlayer(player, trackedEntity);
        });
        ServerTickEvents.END_SERVER_TICK.register(TimeDilationManager::tickServer);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ChronosphereManager.refresh(handler.player);
            TimeDilationManager.resyncPersistentEffects(handler.player);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            ChronosphereManager.refresh(newPlayer);
            TimeDilationManager.resyncPersistentEffects(newPlayer);
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> ChronosphereManager.refresh(player));
    }

    private static <T extends CustomPacketPayload> void registerServerbound(AnthologyNetwork.ServerboundPayload<T> payload) {
        PayloadTypeRegistry.playC2S().register(payload.type(), payload.codec());
        ServerPlayNetworking.registerGlobalReceiver(payload.type(), (message, context) -> {
            if (context.server().isSameThread()) {
                payload.handler().accept(message, context.player());
            } else {
                context.server().execute(() -> payload.handler().accept(message, context.player()));
            }
        });
    }

    private static <T extends CustomPacketPayload> void registerClientboundType(AnthologyNetwork.ClientboundPayload<T> payload) {
        PayloadTypeRegistry.playS2C().register(payload.type(), payload.codec());
    }
}
