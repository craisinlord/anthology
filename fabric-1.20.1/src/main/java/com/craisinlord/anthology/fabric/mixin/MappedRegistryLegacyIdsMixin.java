package com.craisinlord.anthology.fabric.mixin;

import com.craisinlord.anthology.registry.LegacyIds;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MappedRegistry.class)
public abstract class MappedRegistryLegacyIdsMixin<T> {
    @Shadow
    public abstract ResourceKey<? extends Registry<T>> key();

    @ModifyVariable(
            method = {
                    "get(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;",
                    "containsKey(Lnet/minecraft/resources/ResourceLocation;)Z"
            },
            at = @At("HEAD"),
            argsOnly = true
    )
    private ResourceLocation anthology$resolveLegacyLocation(ResourceLocation id) {
        return LegacyIds.resolve(this.key(), id);
    }

    @ModifyVariable(
            method = {
                    "get(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;",
                    "getHolder(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;",
                    "containsKey(Lnet/minecraft/resources/ResourceKey;)Z"
            },
            at = @At("HEAD"),
            argsOnly = true
    )
    private ResourceKey<T> anthology$resolveLegacyKey(ResourceKey<T> key) {
        if (key == null) {
            return null;
        }
        ResourceLocation resolved = LegacyIds.resolve(this.key(), key.location());
        return resolved == key.location() ? key : ResourceKey.create(this.key(), resolved);
    }
}
