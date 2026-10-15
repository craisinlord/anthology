package com.craisinlord.anthology.registry;

import com.craisinlord.anthology.Anthology;
import com.craisinlord.anthology.time.TimeDilationFieldEntity;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class AnthologyEntities {
    private static Supplier<EntityType<TimeDilationFieldEntity>> timeDilationField;

    private AnthologyEntities() {
    }

    @SuppressWarnings("unchecked")
    public static void register(BiFunction<String, Supplier<EntityType<?>>, Supplier<? extends EntityType<?>>> registrar) {
        timeDilationField = (Supplier<EntityType<TimeDilationFieldEntity>>) registrar.apply("time_dilation_field",
                () -> EntityType.Builder.<TimeDilationFieldEntity>of(TimeDilationFieldEntity::new, MobCategory.MISC)
                        .sized(0.5F, 0.5F)
                        .clientTrackingRange(16)
                        .updateInterval(20)
                        .fireImmune()
                        .build(ResourceKey.create(Registries.ENTITY_TYPE, Anthology.id("time_dilation_field"))));
        LegacyIds.add(Registries.ENTITY_TYPE, "time_dilation_field");
    }

    public static EntityType<TimeDilationFieldEntity> timeDilationField() {
        if (timeDilationField == null) {
            ContentBootstrap.ensure();
        }
        return timeDilationField.get();
    }
}
