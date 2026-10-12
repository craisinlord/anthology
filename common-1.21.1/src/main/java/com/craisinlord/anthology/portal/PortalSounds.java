package com.craisinlord.anthology.portal;

import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;

public record PortalSounds(
        @Nullable Supplier<SoundEvent> enter,
        @Nullable Supplier<SoundEvent> exit,
        @Nullable Supplier<SoundEvent> ambient,
        int ambientInterval,
        float ambientVolume
) {
    public static final PortalSounds NONE = new PortalSounds(null, null, null, 80, 0.18F);
}
