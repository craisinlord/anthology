package com.craisinlord.anthology.gravity;

import com.craisinlord.anthology.Anthology;
import io.netty.buffer.ByteBuf;
import com.craisinlord.anthology.network.ByteBufCodecs;
import com.craisinlord.anthology.network.StreamCodec;
import com.craisinlord.anthology.network.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GravityStatePayload(
        int entityId,
        int directionId,
        int previousDirectionId,
        boolean forced,
        int transitionDuration,
        int transitionRemaining
) implements CustomPacketPayload {
    public static final Type<GravityStatePayload> TYPE = new Type<>(new ResourceLocation(Anthology.MODID, "gravity_state"));
    public static final StreamCodec<ByteBuf, GravityStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            GravityStatePayload::entityId,
            ByteBufCodecs.VAR_INT,
            GravityStatePayload::directionId,
            ByteBufCodecs.VAR_INT,
            GravityStatePayload::previousDirectionId,
            ByteBufCodecs.BOOL,
            GravityStatePayload::forced,
            ByteBufCodecs.VAR_INT,
            GravityStatePayload::transitionDuration,
            ByteBufCodecs.VAR_INT,
            GravityStatePayload::transitionRemaining,
            GravityStatePayload::new
    );

    public GravityStatePayload(int entityId, GravityDirection direction, GravityDirection previousDirection, boolean forced, int transitionDuration, int transitionRemaining) {
        this(entityId, direction.getId(), previousDirection.getId(), forced, transitionDuration, transitionRemaining);
    }

    public GravityDirection direction() {
        return GravityDirection.byId(this.directionId);
    }

    public GravityDirection previousDirection() {
        return GravityDirection.byId(this.previousDirectionId);
    }

    @Override
    public Type<GravityStatePayload> type() {
        return TYPE;
    }
}
