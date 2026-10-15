package com.craisinlord.anthology.forge;

import com.craisinlord.anthology.Anthology;
import com.craisinlord.anthology.effect.AnthologyEffects;
import com.craisinlord.anthology.forge.multipart.MultipartPartEntity;
import com.craisinlord.anthology.gravity.GravityCommand;
import com.craisinlord.anthology.gravity.GravitySync;
import com.craisinlord.anthology.multipart.MultipartFramework;
import com.craisinlord.anthology.registry.AnthologyEntities;
import com.craisinlord.anthology.registry.LegacyIds;
import com.craisinlord.anthology.time.ChronosphereManager;
import com.craisinlord.anthology.time.TimeDilationCommand;
import com.craisinlord.anthology.time.TimeDilationManager;
import com.craisinlord.anthology.time.TimeDilationSync;
import com.craisinlord.anthology.travel.CrossDimensionTravelApi;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegisterEvent;

@Mod(Anthology.MODID)
public final class AnthologyForge {
    private static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Anthology.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Anthology.MODID);

    public AnthologyForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        Anthology.init();
        MultipartFramework.setPartFactory((owner, partIndex, spec) -> new MultipartPartEntity((Entity) owner, owner, partIndex, spec));
        AnthologyEffects.register(MOB_EFFECTS::register);
        AnthologyEntities.register(ENTITY_TYPES::register);
        MOB_EFFECTS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        AnthologyForgeNetwork.init();
        CrossDimensionTravelApi.setEntityTransfer((entity, destination, position, yRot, xRot) ->
                entity.changeDimension(destination, new ITeleporter() {
                    @Override
                    public PortalInfo getPortalInfo(Entity moved, ServerLevel level, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                        return new PortalInfo(position, Vec3.ZERO, yRot, xRot);
                    }

                    @Override
                    public boolean playTeleportSound(ServerPlayer player, ServerLevel sourceLevel, ServerLevel destinationLevel) {
                        return false;
                    }
                }));

        modEventBus.addListener(AnthologyForge::registerAliases);
        modEventBus.addListener(AnthologyForge::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(AnthologyForge::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(AnthologyForge::startTracking);
        MinecraftForge.EVENT_BUS.addListener(AnthologyForge::serverTick);
        MinecraftForge.EVENT_BUS.addListener(AnthologyForge::playerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(AnthologyForge::playerRespawn);
        MinecraftForge.EVENT_BUS.addListener(AnthologyForge::playerChangedDimension);
        if (FMLEnvironment.dist.isClient()) {
            AnthologyForgeClient.init(modEventBus);
        }
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(AnthologyForgeNetwork::registerMessages);
    }

    private static void registerAliases(RegisterEvent event) {
        if (event.getForgeRegistry() instanceof ForgeRegistry<?> registry) {
            LegacyIds.aliases(event.getRegistryKey()).forEach(registry::addAlias);
        }
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

    private static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            TimeDilationManager.tickServer(event.getServer());
        }
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
