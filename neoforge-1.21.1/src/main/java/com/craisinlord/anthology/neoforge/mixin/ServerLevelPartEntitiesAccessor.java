package com.craisinlord.anthology.neoforge.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerLevel.class)

public interface ServerLevelPartEntitiesAccessor {
    @Accessor("dragonParts")
    Int2ObjectMap<PartEntity<?>> anthology$getPartEntities();
}
