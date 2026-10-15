package com.craisinlord.anthology.mixins;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class AnthologyMixinPlugin implements IMixinConfigPlugin {
    private static final String COMPAT_PACKAGE = "com.craisinlord.anthology.mixins.compat.";
    private static final Map<String, String> COMPAT_MARKERS = Map.of(
            "scguns", "top.ribs.scguns.entity.projectile.ProjectileEntity",
            "irons_spellbooks", "io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile",
            "ars_nouveau", "com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell",
            "geckolib", "com.geckolib.animation.AnimationController"
    );

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!classPresent(targetClassName)) {
            return false;
        }
        if (mixinClassName.startsWith(COMPAT_PACKAGE)) {
            String compat = mixinClassName.substring(COMPAT_PACKAGE.length());
            int dot = compat.indexOf('.');
            String marker = dot < 0 ? null : COMPAT_MARKERS.get(compat.substring(0, dot));
            return marker == null || classPresent(marker);
        }
        return true;
    }

    private static boolean classPresent(String className) {
        String resourcePath = className.replace('.', '/') + ".class";
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null && contextClassLoader.getResource(resourcePath) != null) {
            return true;
        }
        ClassLoader mixinClassLoader = AnthologyMixinPlugin.class.getClassLoader();
        return mixinClassLoader != null && mixinClassLoader.getResource(resourcePath) != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
