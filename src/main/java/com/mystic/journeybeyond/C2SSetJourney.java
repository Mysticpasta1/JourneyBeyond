package com.mystic.journeybeyond;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record C2SSetJourney(boolean toggle) implements CustomPacketPayload {
    public static final Type<C2SSetJourney> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(JourneyMod.MODID, "set_journey"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSetJourney> STREAM_CODEC =
        CustomPacketPayload.codec((msg, buf) -> buf.writeBoolean(msg.toggle), buf -> new C2SSetJourney(buf.readBoolean()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(C2SSetJourney msg, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ServerPlayer sp = (ServerPlayer) ctx.player();
        ctx.enqueueWork(() -> {
            IJourneyData dat = sp.getCapability(JourneyCapabilities.JOURNEY_DATA, null);
            if (dat == null) return;
            dat.setJourney(!dat.isJourney()); // toggle
            sp.onUpdateAbilities();
            Networking.syncTo(sp);
        });
    }
}
