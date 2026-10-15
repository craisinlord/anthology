package com.craisinlord.anthology.forge;

import com.craisinlord.anthology.Anthology;
import com.craisinlord.anthology.network.AnthologyNetwork;
import com.craisinlord.anthology.network.CustomPacketPayload;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class AnthologyForgeNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(Anthology.id("main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();
    private static boolean registered;

    public record ClientboundMessage(CustomPacketPayload payload) {
    }

    public record ServerboundMessage(CustomPacketPayload payload) {
    }

    private AnthologyForgeNetwork() {
    }

    static void init() {
        AnthologyNetwork.setSender(new AnthologyNetwork.Sender() {
            @Override
            public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundMessage(payload));
            }

            @Override
            public void sendToTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
                CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), new ClientboundMessage(payload));
            }

            @Override
            public void sendToServer(CustomPacketPayload payload) {
                CHANNEL.sendToServer(new ServerboundMessage(payload));
            }
        });
    }

    static synchronized void registerMessages() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.registerMessage(0, ClientboundMessage.class,
                (message, buf) -> write(message.payload(), buf),
                AnthologyForgeNetwork::readClientbound,
                AnthologyForgeNetwork::handleClientbound,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, ServerboundMessage.class,
                (message, buf) -> write(message.payload(), buf),
                AnthologyForgeNetwork::readServerbound,
                AnthologyForgeNetwork::handleServerbound,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    private static void write(CustomPacketPayload payload, FriendlyByteBuf buf) {
        buf.writeResourceLocation(payload.type().id());
        AnthologyNetwork.encode(payload, buf);
    }

    private static ClientboundMessage readClientbound(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        AnthologyNetwork.ClientboundPayload<?> entry = AnthologyNetwork.clientbound(id);
        if (entry == null) {
            throw new IllegalStateException("Unknown Anthology clientbound payload " + id);
        }
        return new ClientboundMessage(entry.decode(buf));
    }

    private static ServerboundMessage readServerbound(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        AnthologyNetwork.ServerboundPayload<?> entry = AnthologyNetwork.serverbound(id);
        if (entry == null) {
            throw new IllegalStateException("Unknown Anthology serverbound payload " + id);
        }
        return new ServerboundMessage(entry.decode(buf));
    }

    private static void handleClientbound(ClientboundMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AnthologyForgeClientNetwork.handle(message.payload())));
        context.setPacketHandled(true);
    }

    private static void handleServerbound(ServerboundMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        AnthologyNetwork.ServerboundPayload<?> entry = AnthologyNetwork.serverbound(message.payload().type().id());
        if (player != null && entry != null) {
            context.enqueueWork(() -> entry.handle(message.payload(), player));
        }
        context.setPacketHandled(true);
    }
}
