package com.craisinlord.anthology.mixins.portal;

import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerEntity.class)
public interface ServerEntityAccessor {
    @Accessor("positionCodec")
    VecDeltaCodec anthology$getPositionCodec();

    @Accessor("lastSentMovement")
    void anthology$setLastSentMovement(Vec3 lastSentMovement);

    @Accessor("updateInterval")
    void anthology$setUpdateInterval(int updateInterval);
}
