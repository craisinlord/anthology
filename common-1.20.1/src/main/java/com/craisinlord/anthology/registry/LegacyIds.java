package com.craisinlord.anthology.registry;

import com.craisinlord.anthology.Anthology;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public final class LegacyIds {
    public static final String LEGACY_NAMESPACE = "antarchy";

    private static final Map<ResourceKey<? extends Registry<?>>, Map<ResourceLocation, ResourceLocation>> ALIASES = new ConcurrentHashMap<>();

    private LegacyIds() {
    }

    public static void add(ResourceKey<? extends Registry<?>> registry, String path) {
        ALIASES.computeIfAbsent(registry, key -> new ConcurrentHashMap<>())
                .put(new ResourceLocation(LEGACY_NAMESPACE, path), Anthology.id(path));
    }

    public static Map<ResourceLocation, ResourceLocation> aliases(ResourceKey<? extends Registry<?>> registry) {
        return ALIASES.getOrDefault(registry, Map.of());
    }

    public static ResourceLocation resolve(ResourceKey<? extends Registry<?>> registry, ResourceLocation id) {
        if (id == null || !LEGACY_NAMESPACE.equals(id.getNamespace())) {
            return id;
        }
        return aliases(registry).getOrDefault(id, id);
    }
}
