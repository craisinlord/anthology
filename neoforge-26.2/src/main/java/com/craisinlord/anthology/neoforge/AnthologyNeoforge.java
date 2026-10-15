package com.craisinlord.anthology.neoforge;

import com.craisinlord.anthology.Anthology;
import com.craisinlord.anthology.effect.AnthologyEffects;
import com.craisinlord.anthology.gravity.GravityCommand;
import com.craisinlord.anthology.gravity.GravitySync;
import com.craisinlord.anthology.multipart.MultipartFramework;
import com.craisinlord.anthology.neoforge.multipart.MultipartPartEntity;
import com.craisinlord.anthology.network.AnthologyNetwork;
import com.craisinlord.anthology.registry.AnthologyEntities;
import com.craisinlord.anthology.registry.LegacyIds;
import com.craisinlord.anthology.time.ChronosphereManager;
import com.craisinlord.anthology.time.TimeDilationCommand;
import com.craisinlord.anthology.time.TimeDilationManager;
import com.craisinlord.anthology.time.TimeDilationSync;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Anthology.MODID)
public final class AnthologyNeoforge {
    private static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Anthology.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Anthology.MODID);

    public AnthologyNeoforge(IEventBus modEventBus) {
        Anthology.init();
        MultipartFramework.setPartFactory((owner, partIndex, spec) -> new MultipartPartEntity((Entity) owner, owner, partIndex, spec));
        AnthologyEffects.register(MOB_EFFECTS::register);
        AnthologyEntities.register(ENTITY_TYPES::register);
        LegacyIds.aliases(Registries.MOB_EFFECT).forEach(MOB_EFFECTS::addAlias);
        LegacyIds.aliases(Registries.ENTITY_TYPE).forEach(ENTITY_TYPES::addAlias);
        MOB_EFFECTS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);

        AnthologyNetwork.setSender(new AnthologyNetwork.Sender() {
            @Override
            public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
                PacketDistributor.sendToPlayer(player, payload);
            }

            @Override
            public void sendToTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
            }

            @Override
            public void sendToServer(CustomPacketPayload payload) {
                AnthologyNeoforgeClient.sendToServer(payload);
            }
        });
        modEventBus.addListener(AnthologyNeoforge::registerPayloads);
        NeoForge.EVENT_BUS.addListener(AnthologyNeoforge::registerCommands);
        NeoForge.EVENT_BUS.addListener(AnthologyNeoforge::startTracking);
        NeoForge.EVENT_BUS.addListener(AnthologyNeoforge::serverTick);
        NeoForge.EVENT_BUS.addListener(AnthologyNeoforge::playerLoggedIn);
        NeoForge.EVENT_BUS.addListener(AnthologyNeoforge::playerRespawn);
        NeoForge.EVENT_BUS.addListener(AnthologyNeoforge::playerChangedDimension);
        if (FMLEnvironment.getDist().isClient()) {
            AnthologyNeoforgeClient.init(modEventBus);
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        for (AnthologyNetwork.ClientboundPayload<?> payload : AnthologyNetwork.clientbound()) {
            registerClientbound(registrar, payload);
        }
        for (AnthologyNetwork.ServerboundPayload<?> payload : AnthologyNetwork.serverbound()) {
            registerServerbound(registrar, payload);
        }
    }

    private static <T extends CustomPacketPayload> void registerServerbound(PayloadRegistrar registrar, AnthologyNetwork.ServerboundPayload<T> payload) {
        registrar.playToServer(payload.type(), payload.codec(), (message, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.level().getServer().isSameThread()) {
                payload.handler().accept(message, player);
            } else {
                context.enqueueWork(() -> payload.handler().accept(message, player));
            }
        });
    }

    private static <T extends CustomPacketPayload> void registerClientbound(PayloadRegistrar registrar, AnthologyNetwork.ClientboundPayload<T> payload) {
        registrar.playToClient(payload.type(), payload.codec(), (message, context) ->
                context.enqueueWork(() -> payload.handler().accept(message, context.player())));
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        GravityCommand.register(event.getDispatcher());
        TimeDilationCommand.register(event.getDispatcher());
    }

    private static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GravitySync.syncToPlayer(player, event.getTarget());
            TimeDilationSync.syncRateToPlayer(player, event.getTarget());
        }
    }

    private static void serverTick(ServerTickEvent.Post event) {
        TimeDilationManager.tickServer(event.getServer());
    }

    private static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GravitySync.syncToPlayer(player, player);
            ChronosphereManager.refresh(player);
            TimeDilationManager.resyncPersistentEffects(player);
        }
    }

    private static void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GravitySync.syncToPlayer(player, player);
            ChronosphereManager.refresh(player);
            TimeDilationManager.resyncPersistentEffects(player);
        }
    }

    private static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GravitySync.syncToPlayer(player, player);
            ChronosphereManager.refresh(player);
        }
    }
}
