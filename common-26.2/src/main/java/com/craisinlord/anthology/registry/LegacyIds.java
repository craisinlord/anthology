package com.craisinlord.anthology.registry;

import com.craisinlord.anthology.Anthology;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

public final class LegacyIds {
    public static final String LEGACY_NAMESPACE = "antarchy";

    private static final Map<ResourceKey<? extends Registry<?>>, Map<Identifier, Identifier>> ALIASES = new ConcurrentHashMap<>();

    private LegacyIds() {
    }

    public static void add(ResourceKey<? extends Registry<?>> registry, String path) {
        ALIASES.computeIfAbsent(registry, key -> new ConcurrentHashMap<>())
                .put(Identifier.fromNamespaceAndPath(LEGACY_NAMESPACE, path), Anthology.id(path));
    }

    public static Map<Identifier, Identifier> aliases(ResourceKey<? extends Registry<?>> registry) {
        return ALIASES.getOrDefault(registry, Map.of());
    }

    public static Identifier resolve(ResourceKey<? extends Registry<?>> registry, Identifier id) {
        if (id == null || !LEGACY_NAMESPACE.equals(id.getNamespace())) {
            return id;
        }
        return aliases(registry).getOrDefault(id, id);
    }
}
