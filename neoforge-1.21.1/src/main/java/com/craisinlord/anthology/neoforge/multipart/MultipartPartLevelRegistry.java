package com.craisinlord.anthology.neoforge.multipart;

import com.craisinlord.anthology.multipart.MultipartEntityOwner;
import com.craisinlord.anthology.neoforge.mixin.ServerLevelPartEntitiesAccessor;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.entity.PartEntity;

public final class MultipartPartLevelRegistry {
    private MultipartPartLevelRegistry() {
    }

    public static void register(MultipartEntityOwner owner) {
        Entity[] parts = owner.anthology$getMultipartParts();
        if (parts == null) {
            return;
        }

        Int2ObjectMap<PartEntity<?>> registry = getRegistry(((Entity) owner).level());
        if (registry == null) {
            return;
        }

        for (Entity part : parts) {
            if (part instanceof PartEntity<?> partEntity) {
                registry.put(partEntity.getId(), partEntity);
            }
        }
    }

    public static void unregister(MultipartEntityOwner owner) {
        Entity[] parts = owner.anthology$getMultipartParts();
        if (parts == null) {
            return;
        }

        Int2ObjectMap<PartEntity<?>> registry = getRegistry(((Entity) owner).level());
        if (registry == null) {
            return;
        }

        for (Entity part : parts) {
            if (part instanceof PartEntity<?> partEntity) {
                registry.remove(partEntity.getId());
            }
        }
    }

    private static Int2ObjectMap<PartEntity<?>> getRegistry(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return ((ServerLevelPartEntitiesAccessor) serverLevel).anthology$getPartEntities();
        }

        if (level.isClientSide()) {
            return ClientPartEntityRegistry.get(level);
        }

        return null;
    }
}
