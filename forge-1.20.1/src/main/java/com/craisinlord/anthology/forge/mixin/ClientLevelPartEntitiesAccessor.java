package com.craisinlord.anthology.forge.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientLevel.class)

public interface ClientLevelPartEntitiesAccessor {
    @Accessor(value = "partEntities", remap = false)
    Int2ObjectMap<PartEntity<?>> anthology$getPartEntities();
}
