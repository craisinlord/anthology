package com.craisinlord.anthology.network;

import java.util.function.BiConsumer;
import java.util.function.Function;

public interface StreamCodec<B, V> {
    V decode(B buf);

    void encode(B buf, V value);

    static <B, V> StreamCodec<B, V> of(BiConsumer<B, V> encoder, Function<B, V> decoder) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                return decoder.apply(buf);
            }

            @Override
            public void encode(B buf, V value) {
                encoder.accept(buf, value);
            }
        };
    }

    static <B, V> StreamCodec<B, V> unit(V value) {
        return of((buf, ignored) -> {
        }, buf -> value);
    }

    default <O> StreamCodec<B, O> map(Function<? super V, ? extends O> to, Function<? super O, ? extends V> from) {
        StreamCodec<B, V> self = this;
        return new StreamCodec<>() {
            @Override
            public O decode(B buf) {
                return to.apply(self.decode(buf));
            }

            @Override
            public void encode(B buf, O value) {
                self.encode(buf, from.apply(value));
            }
        };
    }

    static <B, C1, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, C1> codec1, Function<V, C1> getter1,
            Function<C1, V> factory) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                C1 v1 = codec1.decode(buf);
                return factory.apply(v1);
            }

            @Override
            public void encode(B buf, V value) {
                codec1.encode(buf, getter1.apply(value));
            }
        };
    }

    interface Function2<C1, C2, V> {
        V apply(C1 c1, C2 c2);
    }

    static <B, C1, C2, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, C1> codec1, Function<V, C1> getter1,
            StreamCodec<? super B, C2> codec2, Function<V, C2> getter2,
            Function2<C1, C2, V> factory) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                C1 v1 = codec1.decode(buf);
                C2 v2 = codec2.decode(buf);
                return factory.apply(v1, v2);
            }

            @Override
            public void encode(B buf, V value) {
                codec1.encode(buf, getter1.apply(value));
                codec2.encode(buf, getter2.apply(value));
            }
        };
    }

    interface Function3<C1, C2, C3, V> {
        V apply(C1 c1, C2 c2, C3 c3);
    }

    static <B, C1, C2, C3, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, C1> codec1, Function<V, C1> getter1,
            StreamCodec<? super B, C2> codec2, Function<V, C2> getter2,
            StreamCodec<? super B, C3> codec3, Function<V, C3> getter3,
            Function3<C1, C2, C3, V> factory) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                C1 v1 = codec1.decode(buf);
                C2 v2 = codec2.decode(buf);
                C3 v3 = codec3.decode(buf);
                return factory.apply(v1, v2, v3);
            }

            @Override
            public void encode(B buf, V value) {
                codec1.encode(buf, getter1.apply(value));
                codec2.encode(buf, getter2.apply(value));
                codec3.encode(buf, getter3.apply(value));
            }
        };
    }

    interface Function4<C1, C2, C3, C4, V> {
        V apply(C1 c1, C2 c2, C3 c3, C4 c4);
    }

    static <B, C1, C2, C3, C4, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, C1> codec1, Function<V, C1> getter1,
            StreamCodec<? super B, C2> codec2, Function<V, C2> getter2,
            StreamCodec<? super B, C3> codec3, Function<V, C3> getter3,
            StreamCodec<? super B, C4> codec4, Function<V, C4> getter4,
            Function4<C1, C2, C3, C4, V> factory) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                C1 v1 = codec1.decode(buf);
                C2 v2 = codec2.decode(buf);
                C3 v3 = codec3.decode(buf);
                C4 v4 = codec4.decode(buf);
                return factory.apply(v1, v2, v3, v4);
            }

            @Override
            public void encode(B buf, V value) {
                codec1.encode(buf, getter1.apply(value));
                codec2.encode(buf, getter2.apply(value));
                codec3.encode(buf, getter3.apply(value));
                codec4.encode(buf, getter4.apply(value));
            }
        };
    }

    interface Function5<C1, C2, C3, C4, C5, V> {
        V apply(C1 c1, C2 c2, C3 c3, C4 c4, C5 c5);
    }

    static <B, C1, C2, C3, C4, C5, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, C1> codec1, Function<V, C1> getter1,
            StreamCodec<? super B, C2> codec2, Function<V, C2> getter2,
            StreamCodec<? super B, C3> codec3, Function<V, C3> getter3,
            StreamCodec<? super B, C4> codec4, Function<V, C4> getter4,
            StreamCodec<? super B, C5> codec5, Function<V, C5> getter5,
            Function5<C1, C2, C3, C4, C5, V> factory) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                C1 v1 = codec1.decode(buf);
                C2 v2 = codec2.decode(buf);
                C3 v3 = codec3.decode(buf);
                C4 v4 = codec4.decode(buf);
                C5 v5 = codec5.decode(buf);
                return factory.apply(v1, v2, v3, v4, v5);
            }

            @Override
            public void encode(B buf, V value) {
                codec1.encode(buf, getter1.apply(value));
                codec2.encode(buf, getter2.apply(value));
                codec3.encode(buf, getter3.apply(value));
                codec4.encode(buf, getter4.apply(value));
                codec5.encode(buf, getter5.apply(value));
            }
        };
    }

    interface Function6<C1, C2, C3, C4, C5, C6, V> {
        V apply(C1 c1, C2 c2, C3 c3, C4 c4, C5 c5, C6 c6);
    }

    static <B, C1, C2, C3, C4, C5, C6, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, C1> codec1, Function<V, C1> getter1,
            StreamCodec<? super B, C2> codec2, Function<V, C2> getter2,
            StreamCodec<? super B, C3> codec3, Function<V, C3> getter3,
            StreamCodec<? super B, C4> codec4, Function<V, C4> getter4,
            StreamCodec<? super B, C5> codec5, Function<V, C5> getter5,
            StreamCodec<? super B, C6> codec6, Function<V, C6> getter6,
            Function6<C1, C2, C3, C4, C5, C6, V> factory) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                C1 v1 = codec1.decode(buf);
                C2 v2 = codec2.decode(buf);
                C3 v3 = codec3.decode(buf);
                C4 v4 = codec4.decode(buf);
                C5 v5 = codec5.decode(buf);
                C6 v6 = codec6.decode(buf);
                return factory.apply(v1, v2, v3, v4, v5, v6);
            }

            @Override
            public void encode(B buf, V value) {
                codec1.encode(buf, getter1.apply(value));
                codec2.encode(buf, getter2.apply(value));
                codec3.encode(buf, getter3.apply(value));
                codec4.encode(buf, getter4.apply(value));
                codec5.encode(buf, getter5.apply(value));
                codec6.encode(buf, getter6.apply(value));
            }
        };
    }
}
