package com.craisinlord.anthology.portal;

import net.minecraft.resources.Identifier;

public interface PortalAppearance {
    int color();

    Identifier rimTexture();

    Identifier surfaceTexture(boolean fancy);

    default int surfaceFrames() {
        return 4;
    }
}
