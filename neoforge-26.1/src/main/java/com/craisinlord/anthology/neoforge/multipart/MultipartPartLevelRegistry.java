package com.craisinlord.anthology.neoforge.multipart;

import com.craisinlord.anthology.multipart.MultipartEntityOwner;
import com.craisinlord.anthology.neoforge.mixin.ServerLevelPartEntitiesAccessor;
import java.util.List;
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
        Level level = ((Entity) owner).level();
        for (Entity part : parts) {
            if (part instanceof PartEntity<?> partEntity) {
                add(level, partEntity);
            }
        }
    }

    public static void unregister(MultipartEntityOwner owner) {
        Entity[] parts = owner.anthology$getMultipartParts();
        if (parts == null) {
            return;
        }
        Level level = ((Entity) owner).level();
        for (Entity part : parts) {
            if (part instanceof PartEntity<?> partEntity) {
                remove(level, partEntity);
            }
        }
    }

    private static void add(Level level, PartEntity<?> part) {
        if (level instanceof ServerLevel serverLevel) {
            ((ServerLevelPartEntitiesAccessor) serverLevel).anthology$getPartEntities().put(part.getId(), part);
        } else if (level.isClientSide()) {
            List<PartEntity<?>> parts = ClientPartEntityRegistry.get(level);
            if (parts != null && !parts.contains(part)) {
                parts.add(part);
            }
        }
    }

    private static void remove(Level level, PartEntity<?> part) {
        if (level instanceof ServerLevel serverLevel) {
            ((ServerLevelPartEntitiesAccessor) serverLevel).anthology$getPartEntities().remove(part.getId());
        } else if (level.isClientSide()) {
            List<PartEntity<?>> parts = ClientPartEntityRegistry.get(level);
            if (parts != null) {
                parts.remove(part);
            }
        }
    }
}
