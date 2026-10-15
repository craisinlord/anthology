package com.craisinlord.anthology.fabric;

import com.craisinlord.anthology.Anthology;
import com.craisinlord.anthology.effect.AnthologyEffects;
import com.craisinlord.anthology.registry.AnthologyEntities;
import com.craisinlord.anthology.registry.ContentBootstrap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;

public final class AnthologyFabricContent implements ContentBootstrap {
    @Override
    public void bootstrap() {
        AnthologyEffects.register((name, effect) -> {
            MobEffect registered = Registry.register(BuiltInRegistries.MOB_EFFECT, Anthology.id(name), effect.get());
            return () -> registered;
        });
        AnthologyEntities.register((name, type) -> {
            EntityType<?> registered = Registry.register(BuiltInRegistries.ENTITY_TYPE, Anthology.id(name), type.get());
            return () -> registered;
        });
        AnthologyFabricEntities.register();
    }
}
