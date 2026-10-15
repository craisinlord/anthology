package com.craisinlord.anthology.portal;

import com.craisinlord.anthology.Anthology;
import io.netty.buffer.ByteBuf;
import com.craisinlord.anthology.network.StreamCodec;
import com.craisinlord.anthology.network.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record PortalTransitPayload(int portalId, Vec3 start, Vec3 end, Vec3 velocity) implements CustomPacketPayload {
    public static final Type<PortalTransitPayload> TYPE = new Type<>(new ResourceLocation(Anthology.MODID, "portal_transit"));
    public static final StreamCodec<ByteBuf, PortalTransitPayload> STREAM_CODEC = StreamCodec.of(PortalTransitPayload::write, PortalTransitPayload::read);

    private static void write(ByteBuf buffer, PortalTransitPayload payload) {
        buffer.writeInt(payload.portalId());
        writeVec(buffer, payload.start());
        writeVec(buffer, payload.end());
        writeVec(buffer, payload.velocity());
    }

    private static PortalTransitPayload read(ByteBuf buffer) {
        int portalId = buffer.readInt();
        Vec3 start = readVec(buffer);
        Vec3 end = readVec(buffer);
        Vec3 velocity = readVec(buffer);
        return new PortalTransitPayload(portalId, start, end, velocity);
    }

    private static void writeVec(ByteBuf buffer, Vec3 vector) {
        buffer.writeDouble(vector.x);
        buffer.writeDouble(vector.y);
        buffer.writeDouble(vector.z);
    }

    private static Vec3 readVec(ByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    @Override
    public Type<PortalTransitPayload> type() {
        return TYPE;
    }
}
