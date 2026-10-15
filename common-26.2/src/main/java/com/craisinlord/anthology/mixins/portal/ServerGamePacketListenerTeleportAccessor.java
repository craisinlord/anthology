package com.craisinlord.anthology.mixins.portal;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerTeleportAccessor {
    @Accessor("awaitingPositionFromClient")
    Vec3 anthology$getAwaitingPositionFromClient();
}
