package com.craisinlord.anthology.network;

import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.function.IntFunction;
import net.minecraft.network.FriendlyByteBuf;

public final class ByteBufCodecs {
    public static final StreamCodec<ByteBuf, Integer> VAR_INT = StreamCodec.of(
            (buf, value) -> friendly(buf).writeVarInt(value),
            buf -> friendly(buf).readVarInt()
    );
    public static final StreamCodec<ByteBuf, Long> VAR_LONG = StreamCodec.of(
            (buf, value) -> friendly(buf).writeVarLong(value),
            buf -> friendly(buf).readVarLong()
    );
    public static final StreamCodec<ByteBuf, Boolean> BOOL = StreamCodec.of(ByteBuf::writeBoolean, ByteBuf::readBoolean);
    public static final StreamCodec<ByteBuf, Byte> BYTE = StreamCodec.of((buf, value) -> buf.writeByte(value), ByteBuf::readByte);
    public static final StreamCodec<ByteBuf, Integer> INT = StreamCodec.of(ByteBuf::writeInt, ByteBuf::readInt);
    public static final StreamCodec<ByteBuf, Float> FLOAT = StreamCodec.of(ByteBuf::writeFloat, ByteBuf::readFloat);
    public static final StreamCodec<ByteBuf, Double> DOUBLE = StreamCodec.of(ByteBuf::writeDouble, ByteBuf::readDouble);
    public static final StreamCodec<ByteBuf, String> STRING_UTF8 = StreamCodec.of(
            (buf, value) -> friendly(buf).writeUtf(value),
            buf -> friendly(buf).readUtf()
    );

    private ByteBufCodecs() {
    }

    public static <V, C extends Collection<V>> StreamCodec<ByteBuf, C> collection(IntFunction<C> factory, StreamCodec<? super ByteBuf, V> elementCodec, int maxSize) {
        return StreamCodec.of(
                (buf, values) -> {
                    if (values.size() > maxSize) {
                        throw new IllegalArgumentException("Collection too large: " + values.size() + " > " + maxSize);
                    }
                    friendly(buf).writeVarInt(values.size());
                    for (V value : values) {
                        elementCodec.encode(buf, value);
                    }
                },
                buf -> {
                    int size = friendly(buf).readVarInt();
                    if (size > maxSize) {
                        throw new IllegalArgumentException("Collection too large: " + size + " > " + maxSize);
                    }
                    C values = factory.apply(Math.min(size, 65536));
                    for (int i = 0; i < size; i++) {
                        values.add(elementCodec.decode(buf));
                    }
                    return values;
                }
        );
    }

    private static FriendlyByteBuf friendly(ByteBuf buf) {
        return buf instanceof FriendlyByteBuf friendlyByteBuf ? friendlyByteBuf : new FriendlyByteBuf(buf);
    }
}
