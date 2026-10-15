package com.craisinlord.anthology.mixins.gravity.client;

import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.gravity.GravityDirection;
import com.craisinlord.anthology.gravity.GravityRotationUtil;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerGravityMixin {
    @ModifyExpressionValue(method = "handleExplosion",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ClientboundExplodePacket;playerKnockback()Ljava/util/Optional;"))
    private Optional<Vec3> anthology$fixExplosionKnockback(Optional<Vec3> knockback) {
        if (Minecraft.getInstance().player == null) {
            return knockback;
        }
        GravityDirection gravity = GravityApi.getGravityDirection(Minecraft.getInstance().player);
        if (!gravity.isInverted()) {
            return knockback;
        }
        return knockback.map(value -> GravityRotationUtil.vecWorldToPlayer(value, gravity));
    }
}
