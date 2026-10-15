package com.craisinlord.anthology.neoforge.multipart;

import com.craisinlord.anthology.neoforge.mixin.ClientLevelPartEntitiesAccessor;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.entity.PartEntity;

public final class ClientPartEntityRegistry {
    private ClientPartEntityRegistry() {
    }

    public static List<PartEntity<?>> get(Level level) {
        if (level instanceof ClientLevel clientLevel) {
            return ((ClientLevelPartEntitiesAccessor) clientLevel).anthology$getPartEntities();
        }
        return null;
    }
}
