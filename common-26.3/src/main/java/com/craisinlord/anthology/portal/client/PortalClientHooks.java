package com.craisinlord.anthology.portal.client;

import com.craisinlord.anthology.portal.PortalEntity;
import com.craisinlord.anthology.portal.PortalShape;
import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class PortalClientHooks {
    @FunctionalInterface
    public interface CustomView {
        void render(Minecraft minecraft, RenderTarget target, PortalShape source, Vec3 look, Vec3 up, Matrix4f projection);
    }

    @FunctionalInterface
    public interface OutlinePolicy {
        boolean shouldOutline(PortalEntity portal, LocalPlayer viewer);
    }

    private record CustomViewEntry(Predicate<PortalEntity> applies, CustomView view) {
    }

    private static final List<CustomViewEntry> CUSTOM_VIEWS = new CopyOnWriteArrayList<>();
    private static volatile OutlinePolicy outlinePolicy = (portal, viewer) -> false;

    private PortalClientHooks() {
    }

    public static void registerCustomView(Predicate<PortalEntity> applies, CustomView view) {
        CUSTOM_VIEWS.add(new CustomViewEntry(applies, view));
    }

    public static CustomView customView(PortalEntity portal) {
        if (!portal.hasCustomView()) {
            return null;
        }
        for (CustomViewEntry entry : CUSTOM_VIEWS) {
            if (entry.applies().test(portal)) {
                return entry.view();
            }
        }
        return null;
    }

    public static void setOutlinePolicy(OutlinePolicy policy) {
        outlinePolicy = policy == null ? (portal, viewer) -> false : policy;
    }

    public static OutlinePolicy outlinePolicy() {
        return outlinePolicy;
    }
}
