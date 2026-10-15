package com.craisinlord.anthology.fabric;

import com.craisinlord.anthology.Anthology;
import com.craisinlord.anthology.fabric.multipart.MultipartPartEntity;
import com.craisinlord.anthology.gravity.GravityCommand;
import com.craisinlord.anthology.gravity.GravitySync;
import com.craisinlord.anthology.multipart.MultipartFramework;
import com.craisinlord.anthology.network.AnthologyNetwork;
import com.craisinlord.anthology.network.CustomPacketPayload;
import com.craisinlord.anthology.registry.ContentBootstrap;
import com.craisinlord.anthology.time.ChronosphereManager;
import com.craisinlord.anthology.time.TimeDilationCommand;
import com.craisinlord.anthology.time.TimeDilationManager;
import com.craisinlord.anthology.time.TimeDilationSync;
import com.craisinlord.anthology.travel.CrossDimensionTravelApi;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;

public final class AnthologyFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Anthology.init();
        ContentBootstrap.ensure();
        MultipartFramework.setPartFactory((owner, partIndex, spec) ->
                new MultipartPartEntity(AnthologyFabricEntities.MULTIPART_PART, ((Entity) owner).level()).anthology$configure(owner, partIndex, spec));
        CrossDimensionTravelApi.setEntityTransfer((entity, destination, position, yRot, xRot) ->
                FabricDimensions.teleport(entity, destination, new PortalInfo(position, Vec3.ZERO, yRot, xRot)));

        AnthologyNetwork.setSender(new AnthologyNetwork.Sender() {
            @Override
            public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
                ServerPlayNetworking.send(player, payload.type().id(), encode(payload));
            }

            @Override
            public void sendToTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
                for (ServerPlayer player : PlayerLookup.tracking(entity)) {
                    if (player != entity) {
                        ServerPlayNetworking.send(player, payload.type().id(), encode(payload));
                    }
                }
                if (entity instanceof ServerPlayer self) {
                    ServerPlayNetworking.send(self, payload.type().id(), encode(payload));
                }
            }

            @Override
            public void sendToServer(CustomPacketPayload payload) {
                AnthologyFabricClientBridge.sendToServer(payload);
            }
        });
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
            GravitySync.syncToPlayer(handler.player, handler.player);
            ChronosphereManager.refresh(handler.player);
            TimeDilationManager.resyncPersistentEffects(handler.player);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            GravitySync.syncToPlayer(newPlayer, newPlayer);
            ChronosphereManager.refresh(newPlayer);
            TimeDilationManager.resyncPersistentEffects(newPlayer);
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            GravitySync.syncToPlayer(player, player);
            ChronosphereManager.refresh(player);
        });
    }

    public static FriendlyByteBuf encode(CustomPacketPayload payload) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        AnthologyNetwork.encode(payload, buf);
        return buf;
    }

    private static void registerServerbound(AnthologyNetwork.ServerboundPayload<?> payload) {
        ServerPlayNetworking.registerGlobalReceiver(payload.type().id(), (server, player, handler, buf, responseSender) -> {
            CustomPacketPayload message = payload.decode(buf);
            if (server.isSameThread()) {
                payload.handle(message, player);
            } else {
                server.execute(() -> payload.handle(message, player));
            }
        });
    }
}
