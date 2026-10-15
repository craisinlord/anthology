package com.craisinlord.anthology.multipart;

import com.craisinlord.anthology.Anthology;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MultipartAttackPayload(UUID parentId, int partIndex, float damage) implements CustomPacketPayload {
    public static final Type<MultipartAttackPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Anthology.MODID, "multipart_attack"));
    public static final StreamCodec<ByteBuf, MultipartAttackPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
            MultipartAttackPayload::parentId,
            ByteBufCodecs.VAR_INT,
            MultipartAttackPayload::partIndex,
            ByteBufCodecs.FLOAT,
            MultipartAttackPayload::damage,
            MultipartAttackPayload::new
    );

    @Override
    public Type<MultipartAttackPayload> type() {
        return TYPE;
    }
}
