package com.craisinlord.anthology.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class AnthologyNetwork {
    public record ClientboundPayload<T extends CustomPacketPayload>(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super FriendlyByteBuf, T> codec,
            BiConsumer<T, Player> handler
    ) {
        public T decode(FriendlyByteBuf buf) {
            return this.codec.decode(buf);
        }

        @SuppressWarnings("unchecked")
        public void handle(CustomPacketPayload payload, Player player) {
            this.handler.accept((T) payload, player);
        }
    }

    public record ServerboundPayload<T extends CustomPacketPayload>(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super FriendlyByteBuf, T> codec,
            BiConsumer<T, ServerPlayer> handler
    ) {
        public T decode(FriendlyByteBuf buf) {
            return this.codec.decode(buf);
        }

        @SuppressWarnings("unchecked")
        public void handle(CustomPacketPayload payload, ServerPlayer player) {
            this.handler.accept((T) payload, player);
        }
    }

    public interface Sender {
        void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

        void sendToTrackingAndSelf(Entity entity, CustomPacketPayload payload);

        void sendToServer(CustomPacketPayload payload);
    }

    private static final List<ClientboundPayload<?>> CLIENTBOUND = new ArrayList<>();
    private static final List<ServerboundPayload<?>> SERVERBOUND = new ArrayList<>();
    private static final Map<ResourceLocation, ClientboundPayload<?>> CLIENTBOUND_BY_ID = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ServerboundPayload<?>> SERVERBOUND_BY_ID = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, StreamCodec<? super FriendlyByteBuf, ?>> CODECS = new ConcurrentHashMap<>();
    private static final List<Consumer<ClientboundPayload<?>>> CLIENTBOUND_LISTENERS = new ArrayList<>();
    private static final List<Consumer<ServerboundPayload<?>>> SERVERBOUND_LISTENERS = new ArrayList<>();
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
            StreamCodec<? super FriendlyByteBuf, T> codec,
            BiConsumer<T, Player> handler
    ) {
        ClientboundPayload<T> payload = new ClientboundPayload<>(type, codec, handler);
        List<Consumer<ClientboundPayload<?>>> listeners;
        synchronized (CLIENTBOUND) {
            CLIENTBOUND.add(payload);
            CLIENTBOUND_BY_ID.put(type.id(), payload);
            CODECS.put(type.id(), codec);
            listeners = new ArrayList<>(CLIENTBOUND_LISTENERS);
        }
        listeners.forEach(listener -> listener.accept(payload));
    }

    public static <T extends CustomPacketPayload> void registerServerbound(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super FriendlyByteBuf, T> codec,
            BiConsumer<T, ServerPlayer> handler
    ) {
        ServerboundPayload<T> payload = new ServerboundPayload<>(type, codec, handler);
        List<Consumer<ServerboundPayload<?>>> listeners;
        synchronized (SERVERBOUND) {
            SERVERBOUND.add(payload);
            SERVERBOUND_BY_ID.put(type.id(), payload);
            CODECS.put(type.id(), codec);
            listeners = new ArrayList<>(SERVERBOUND_LISTENERS);
        }
        listeners.forEach(listener -> listener.accept(payload));
    }

    public static void onClientbound(Consumer<ClientboundPayload<?>> listener) {
        List<ClientboundPayload<?>> existing;
        synchronized (CLIENTBOUND) {
            CLIENTBOUND_LISTENERS.add(listener);
            existing = new ArrayList<>(CLIENTBOUND);
        }
        existing.forEach(listener);
    }

    public static void onServerbound(Consumer<ServerboundPayload<?>> listener) {
        List<ServerboundPayload<?>> existing;
        synchronized (SERVERBOUND) {
            SERVERBOUND_LISTENERS.add(listener);
            existing = new ArrayList<>(SERVERBOUND);
        }
        existing.forEach(listener);
    }

    public static List<ClientboundPayload<?>> clientbound() {
        return Collections.unmodifiableList(CLIENTBOUND);
    }

    public static List<ServerboundPayload<?>> serverbound() {
        return Collections.unmodifiableList(SERVERBOUND);
    }

    public static ClientboundPayload<?> clientbound(ResourceLocation id) {
        return CLIENTBOUND_BY_ID.get(id);
    }

    public static ServerboundPayload<?> serverbound(ResourceLocation id) {
        return SERVERBOUND_BY_ID.get(id);
    }

    @SuppressWarnings("unchecked")
    public static void encode(CustomPacketPayload payload, FriendlyByteBuf buf) {
        StreamCodec<? super FriendlyByteBuf, CustomPacketPayload> codec =
                (StreamCodec<? super FriendlyByteBuf, CustomPacketPayload>) CODECS.get(payload.type().id());
        if (codec == null) {
            throw new IllegalStateException("Unregistered Anthology payload " + payload.type().id());
        }
        codec.encode(buf, payload);
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
