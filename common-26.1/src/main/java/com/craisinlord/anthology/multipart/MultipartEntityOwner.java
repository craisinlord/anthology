package com.craisinlord.anthology.multipart;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public interface MultipartEntityOwner {
    MultipartLayout anthology$getMultipartLayout();

    Entity[] anthology$getMultipartParts();

    void anthology$setMultipartParts(Entity[] parts);

    default Entity[] anthology$createMultipartParts() {
        return MultipartFramework.createMultipartParts(this);
    }

    default void anthology$spawnMultipartParts() {
        MultipartFramework.spawnMultipartParts(this);
    }

    default void anthology$discardMultipartParts() {
        MultipartFramework.discardMultipartParts(this);
    }

    default void anthology$syncMultipartParts() {
        MultipartFramework.syncMultipartParts(this);
    }

    default boolean anthology$hurtMultipartPart(Entity part, DamageSource source, float amount) {
        return ((Entity) this).level() instanceof ServerLevel serverLevel && ((Entity) this).hurtServer(serverLevel, source, amount);
    }

    default InteractionResult anthology$interactMultipartPart(Entity part, Player player, Vec3 hitPosition, InteractionHand hand) {
        return ((Entity) this).interact(player, hand, hitPosition);
    }
}
