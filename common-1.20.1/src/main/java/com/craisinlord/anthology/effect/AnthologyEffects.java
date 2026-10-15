package com.craisinlord.anthology.effect;

import com.craisinlord.anthology.registry.ContentBootstrap;
import com.craisinlord.anthology.registry.LegacyIds;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;

public final class AnthologyEffects {
    private static Supplier<? extends MobEffect> inverted;
    private static Supplier<? extends MobEffect> dilated;
    private static Supplier<? extends MobEffect> contracted;

    private AnthologyEffects() {
    }

    public static void register(BiFunction<String, Supplier<MobEffect>, Supplier<? extends MobEffect>> registrar) {
        inverted = registrar.apply("inverted", InvertedMobEffect::new);
        dilated = registrar.apply("dilated", DilatedMobEffect::new);
        contracted = registrar.apply("contracted", ContractedMobEffect::new);
        LegacyIds.add(Registries.MOB_EFFECT, "inverted");
        LegacyIds.add(Registries.MOB_EFFECT, "dilated");
        LegacyIds.add(Registries.MOB_EFFECT, "contracted");
    }

    public static MobEffect inverted() {
        ensureRegistered();
        return inverted.get();
    }

    public static MobEffect dilated() {
        ensureRegistered();
        return dilated.get();
    }

    public static MobEffect contracted() {
        ensureRegistered();
        return contracted.get();
    }

    private static void ensureRegistered() {
        if (inverted == null) {
            ContentBootstrap.ensure();
        }
    }
}
