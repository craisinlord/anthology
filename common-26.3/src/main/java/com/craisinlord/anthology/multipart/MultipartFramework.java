package com.craisinlord.anthology.multipart;

import com.craisinlord.anthology.Anthology;
import com.craisinlord.anthology.network.AnthologyNetwork;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class MultipartFramework {
    private static final double MAX_ATTACK_DISTANCE_SQR = 10000.0D;
    private static boolean warnedMissingFactory;

    public interface PartFactory {
        Entity create(MultipartEntityOwner owner, int partIndex, MultipartPartDefinition spec);
    }

    @Nullable
    private static PartFactory partFactory;

    private MultipartFramework() {
    }

    public static void register() {
        AnthologyNetwork.registerServerbound(MultipartAttackPayload.TYPE, MultipartAttackPayload.STREAM_CODEC, MultipartFramework::handleAttack);
        AnthologyNetwork.registerServerbound(MultipartInteractPayload.TYPE, MultipartInteractPayload.STREAM_CODEC, MultipartFramework::handleInteract);
    }

    public static void setPartFactory(PartFactory factory) {
        partFactory = Objects.requireNonNull(factory, "factory");
    }

    public static Entity[] createMultipartParts(MultipartEntityOwner owner) {
        if (partFactory == null) {
            if (!warnedMissingFactory) {
                warnedMissingFactory = true;
                Anthology.LOGGER.warn("Multipart part factory has not been set on this platform; multipart entities will fall back to their base hitbox only");
            }
            return new Entity[0];
        }
        MultipartPartDefinition[] parts = owner.anthology$getMultipartLayout().parts();
        Entity[] created = new Entity[parts.length];
        for (int i = 0; i < parts.length; i++) {
            created[i] = createMultipartPart(owner, i, parts[i]);
        }
        return created;
    }

    public static Entity createMultipartPart(MultipartEntityOwner owner, int index, MultipartPartDefinition spec) {
        PartFactory factory = partFactory;
        if (factory == null) {
            throw new IllegalStateException("Multipart part factory has not been set");
        }
        return factory.create(owner, index, spec);
    }

    public static Entity[] ensureParts(MultipartEntityOwner owner) {
        Entity[] parts = owner.anthology$getMultipartParts();
        if (parts == null) {
            parts = owner.anthology$createMultipartParts();
            owner.anthology$setMultipartParts(parts);
            owner.anthology$syncMultipartParts();
        }
        return parts;
    }

    public static void spawnMultipartParts(MultipartEntityOwner owner) {
        if (owner.anthology$getMultipartParts() == null) {
            owner.anthology$setMultipartParts(owner.anthology$createMultipartParts());
        }
        syncMultipartParts(owner);
    }

    public static void discardMultipartParts(MultipartEntityOwner owner) {
        Entity[] parts = owner.anthology$getMultipartParts();
        if (parts == null) {
            return;
        }
        for (Entity part : parts) {
            if (part != null) {
                part.discard();
            }
        }
        owner.anthology$setMultipartParts(null);
    }

    public static void syncMultipartParts(MultipartEntityOwner owner) {
        Entity[] parts = owner.anthology$getMultipartParts();
        if (parts == null) {
            return;
        }
        for (Entity part : parts) {
            if (part instanceof MultipartPartAccess access) {
                access.anthology$syncFromParent();
            }
        }
    }

    public static void sendMultipartAttack(UUID parentId, int partIndex, float damage) {
        AnthologyNetwork.sendToServer(new MultipartAttackPayload(parentId, partIndex, damage));
    }

    public static void sendMultipartInteract(UUID parentId, int partIndex, int handId) {
        AnthologyNetwork.sendToServer(new MultipartInteractPayload(parentId, partIndex, handId));
    }

    @Nullable
    private static Entity findPart(ServerPlayer player, UUID parentId, int partIndex) {
        if (!(player.level().getEntity(parentId) instanceof MultipartEntityOwner owner)) {
            return null;
        }
        Entity[] parts = ensureParts(owner);
        if (partIndex < 0 || partIndex >= parts.length) {
            return null;
        }
        Entity part = parts[partIndex];
        return part == null || part.isRemoved() ? null : part;
    }

    private static void handleAttack(MultipartAttackPayload payload, ServerPlayer player) {
        Entity part = findPart(player, payload.parentId(), payload.partIndex());
        if (part != null && player.distanceToSqr(part) <= MAX_ATTACK_DISTANCE_SQR) {
            player.attack(part);
        }
    }

    private static void handleInteract(MultipartInteractPayload payload, ServerPlayer player) {
        Entity part = findPart(player, payload.parentId(), payload.partIndex());
        if (part != null && player.level().getEntity(payload.parentId()) instanceof MultipartEntityOwner owner) {
            InteractionHand hand = payload.handId() == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            owner.anthology$interactMultipartPart(part, player, part.position(), hand);
        }
    }
}
