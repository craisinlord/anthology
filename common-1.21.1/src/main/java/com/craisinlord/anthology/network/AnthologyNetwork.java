package com.craisinlord.anthology.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class AnthologyNetwork {
    public record ClientboundPayload<T extends CustomPacketPayload>(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, Player> handler
    ) {
    }

    public record ServerboundPayload<T extends CustomPacketPayload>(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, ServerPlayer> handler
    ) {
    }

    public interface Sender {
        void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

        void sendToTrackingAndSelf(Entity entity, CustomPacketPayload payload);

        void sendToServer(CustomPacketPayload payload);
    }

    private static final List<ClientboundPayload<?>> CLIENTBOUND = new ArrayList<>();
    private static final List<ServerboundPayload<?>> SERVERBOUND = new ArrayList<>();
    private static Sender sender = new Sender() {
        @Override
        public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        }

        @Override
        public void sendToTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
        }

        @Override
        public void sendToServer(CustomPacketPayload payload) {
        }
    };

    private AnthologyNetwork() {
    }

    public static <T extends CustomPacketPayload> void registerClientbound(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, Player> handler
    ) {
        CLIENTBOUND.add(new ClientboundPayload<>(type, codec, handler));
    }

    public static <T extends CustomPacketPayload> void registerServerbound(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, ServerPlayer> handler
    ) {
        SERVERBOUND.add(new ServerboundPayload<>(type, codec, handler));
    }

    public static List<ClientboundPayload<?>> clientbound() {
        return Collections.unmodifiableList(CLIENTBOUND);
    }

    public static List<ServerboundPayload<?>> serverbound() {
        return Collections.unmodifiableList(SERVERBOUND);
    }

    public static void setSender(Sender newSender) {
        sender = newSender;
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        sender.sendToPlayer(player, payload);
    }

    public static void sendToTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
        sender.sendToTrackingAndSelf(entity, payload);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        sender.sendToServer(payload);
    }
}
