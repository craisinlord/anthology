package com.craisinlord.anthology.fabric;

import com.craisinlord.anthology.Anthology;
import com.craisinlord.anthology.fabric.multipart.MultipartPartEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class AnthologyFabricEntities {
    public static EntityType<MultipartPartEntity> MULTIPART_PART;

    private AnthologyFabricEntities() {
    }

    static void register() {
        MULTIPART_PART = Registry.register(BuiltInRegistries.ENTITY_TYPE, Anthology.id("multipart_part"),
                EntityType.Builder.<MultipartPartEntity>of(MultipartPartEntity::new, MobCategory.MISC)
                        .sized(1.0F, 1.0F)
                        .clientTrackingRange(12)
                        .updateInterval(1)
                        .build(ResourceKey.create(Registries.ENTITY_TYPE, Anthology.id("multipart_part"))));
    }
}
