package com.craisinlord.anthology.time;

import com.craisinlord.anthology.Anthology;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TimeDilationRatePayload(UUID entityUuid, double rate) implements CustomPacketPayload {
    public static final Type<TimeDilationRatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Anthology.MODID, "time_dilation_rate"));
    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, uuid) -> {
                buf.writeLong(uuid.getMostSignificantBits());
                buf.writeLong(uuid.getLeastSignificantBits());
            },
            buf -> new UUID(buf.readLong(), buf.readLong())
    );
    public static final StreamCodec<ByteBuf, TimeDilationRatePayload> STREAM_CODEC = StreamCodec.composite(
            UUID_CODEC,
            TimeDilationRatePayload::entityUuid,
            ByteBufCodecs.DOUBLE,
            TimeDilationRatePayload::rate,
            TimeDilationRatePayload::new
    );

    @Override
    public Type<TimeDilationRatePayload> type() {
        return TYPE;
    }
}
