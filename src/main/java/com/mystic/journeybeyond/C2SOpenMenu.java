package com.mystic.journeybeyond;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2SOpenMenu() implements CustomPacketPayload {
    public static final Type<C2SOpenMenu> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JourneyMod.MODID, "open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SOpenMenu> STREAM_CODEC =
            CustomPacketPayload.codec((buf, msg) -> {}, buf -> new C2SOpenMenu());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(C2SOpenMenu msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = (ServerPlayer) ctx.player();
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inv, p) -> new JourneyMenu(id, inv),
                    Component.translatable("screen.journey.title")
            ));
        });

    }
}
