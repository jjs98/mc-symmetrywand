package com.symmetrywand.network;

import com.symmetrywand.SymmetryWandMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Sent client -> server when the player saves their axis selection in the mirror-axes screen. */
public record MirrorAxesPayload(boolean mirrorX, boolean mirrorY, boolean mirrorZ, boolean mainHand) implements CustomPacketPayload {

    public static final Type<MirrorAxesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SymmetryWandMod.MODID, "mirror_axes"));

    public static final StreamCodec<ByteBuf, MirrorAxesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, MirrorAxesPayload::mirrorX,
            ByteBufCodecs.BOOL, MirrorAxesPayload::mirrorY,
            ByteBufCodecs.BOOL, MirrorAxesPayload::mirrorZ,
            ByteBufCodecs.BOOL, MirrorAxesPayload::mainHand,
            MirrorAxesPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
