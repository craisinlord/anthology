package com.craisinlord.anthology.neoforge.mixin;

import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientLevel.class)

public interface ClientLevelPartEntitiesAccessor {
    @Accessor("dragonParts")
    List<PartEntity<?>> anthology$getPartEntities();
}
