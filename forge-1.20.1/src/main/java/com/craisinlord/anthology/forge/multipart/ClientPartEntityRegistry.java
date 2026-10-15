package com.craisinlord.anthology.forge.multipart;

import com.craisinlord.anthology.forge.mixin.ClientLevelPartEntitiesAccessor;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.entity.PartEntity;

public final class ClientPartEntityRegistry {
    private ClientPartEntityRegistry() {
    }

    public static Int2ObjectMap<PartEntity<?>> get(Level level) {
        if (level instanceof ClientLevel clientLevel) {
            return ((ClientLevelPartEntitiesAccessor) clientLevel).anthology$getPartEntities();
        }
        return null;
    }
}
