package com.craisinlord.anthology.fabric.mixin;

import com.craisinlord.anthology.registry.LegacyIds;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
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
                    "getValue(Lnet/minecraft/resources/Identifier;)Ljava/lang/Object;",
                    "get(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;",
                    "containsKey(Lnet/minecraft/resources/Identifier;)Z"
            },
            at = @At("HEAD"),
            argsOnly = true
    )
    private Identifier anthology$resolveLegacyLocation(Identifier id) {
        return LegacyIds.resolve(this.key(), id);
    }

    @ModifyVariable(
            method = {
                    "getValue(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;",
                    "get(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;",
                    "containsKey(Lnet/minecraft/resources/ResourceKey;)Z"
            },
            at = @At("HEAD"),
            argsOnly = true
    )
    private ResourceKey<T> anthology$resolveLegacyKey(ResourceKey<T> key) {
        if (key == null) {
            return null;
        }
        Identifier resolved = LegacyIds.resolve(this.key(), key.identifier());
        return resolved == key.identifier() ? key : ResourceKey.create(this.key(), resolved);
    }
}
