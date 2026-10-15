package com.craisinlord.anthology.multipart;

import com.craisinlord.anthology.Anthology;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import com.craisinlord.anthology.network.ByteBufCodecs;
import com.craisinlord.anthology.network.StreamCodec;
import com.craisinlord.anthology.network.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MultipartInteractPayload(UUID parentId, int partIndex, int handId) implements CustomPacketPayload {
    public static final Type<MultipartInteractPayload> TYPE =
            new Type<>(new ResourceLocation(Anthology.MODID, "multipart_interact"));
    public static final StreamCodec<ByteBuf, MultipartInteractPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
            MultipartInteractPayload::parentId,
            ByteBufCodecs.VAR_INT,
            MultipartInteractPayload::partIndex,
            ByteBufCodecs.VAR_INT,
            MultipartInteractPayload::handId,
            MultipartInteractPayload::new
    );

    @Override
    public Type<MultipartInteractPayload> type() {
        return TYPE;
    }
}
