package com.craisinlord.anthology.effect;

import com.craisinlord.anthology.registry.ContentBootstrap;
import com.craisinlord.anthology.registry.LegacyIds;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;

public final class AnthologyEffects {
    private static Holder<MobEffect> inverted;
    private static Holder<MobEffect> dilated;
    private static Holder<MobEffect> contracted;

    private AnthologyEffects() {
    }

    public static void register(BiFunction<String, Supplier<MobEffect>, Holder<MobEffect>> registrar) {
        inverted = registrar.apply("inverted", InvertedMobEffect::new);
        dilated = registrar.apply("dilated", DilatedMobEffect::new);
        contracted = registrar.apply("contracted", ContractedMobEffect::new);
        LegacyIds.add(Registries.MOB_EFFECT, "inverted");
        LegacyIds.add(Registries.MOB_EFFECT, "dilated");
        LegacyIds.add(Registries.MOB_EFFECT, "contracted");
    }

    public static Holder<MobEffect> inverted() {
        ensureRegistered();
        return inverted;
    }

    public static Holder<MobEffect> dilated() {
        ensureRegistered();
        return dilated;
    }

    public static Holder<MobEffect> contracted() {
        ensureRegistered();
        return contracted;
    }

    private static void ensureRegistered() {
        if (inverted == null) {
            ContentBootstrap.ensure();
        }
    }
}
