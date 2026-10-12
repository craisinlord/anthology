package com.craisinlord.anthology.time;

import com.craisinlord.anthology.Anthology;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class TimeDilationTags {
    public static final TagKey<EntityType<?>> TIME_DILATION_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Anthology.id("time_dilation_immune"));

    private TimeDilationTags() {
    }
}
