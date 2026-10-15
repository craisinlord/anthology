package com.craisinlord.anthology.forge.mixin;

import com.craisinlord.anthology.multipart.MultipartEntityOwner;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)

public abstract class MultipartEntityBridgeMixin {
    @Unique
    private Entity[] anthology$multipartSourceParts;

    @Unique
    private PartEntity<?>[] anthology$multipartBridgeParts;

    public boolean isMultipartEntity() {
        return (Object) this instanceof MultipartEntityOwner;
    }

    @Nullable
    public PartEntity<?>[] getParts() {
        if (!((Object) this instanceof MultipartEntityOwner owner)) {
            return null;
        }

        Entity[] parts = owner.anthology$getMultipartParts();
        if (parts == null) {
            parts = owner.anthology$createMultipartParts();
            owner.anthology$setMultipartParts(parts);
            owner.anthology$syncMultipartParts();
        }

        if (parts.length == 0) {
            return new PartEntity<?>[0];
        }

        if (this.anthology$multipartSourceParts != parts || this.anthology$multipartBridgeParts == null || this.anthology$multipartBridgeParts.length != parts.length) {
            PartEntity<?>[] bridgedParts = new PartEntity<?>[parts.length];
            for (int i = 0; i < parts.length; i++) {
                if (!(parts[i] instanceof PartEntity<?> part)) {
                    throw new IllegalStateException("Multipart owner returned a non-PartEntity child: " + parts[i]);
                }
                bridgedParts[i] = part;
            }
            this.anthology$multipartSourceParts = parts;
            this.anthology$multipartBridgeParts = bridgedParts;
        }

        return this.anthology$multipartBridgeParts;
    }
}
