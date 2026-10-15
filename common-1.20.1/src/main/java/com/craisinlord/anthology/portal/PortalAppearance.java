package com.craisinlord.anthology.portal;

import net.minecraft.resources.ResourceLocation;

public interface PortalAppearance {
    int color();

    ResourceLocation rimTexture();

    ResourceLocation surfaceTexture(boolean fancy);

    default int surfaceFrames() {
        return 4;
    }
}
