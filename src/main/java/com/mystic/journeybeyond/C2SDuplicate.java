package com.mystic.journeybeyond;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record C2SDuplicate(int itemId, int count) implements CustomPacketPayload {
    public static final Type<C2SDuplicate> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JourneyMod.MODID, "dup"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SDuplicate> STREAM_CODEC =
            CustomPacketPayload.codec(
                    (msg, buf) -> {
                        buf.writeVarInt(msg.itemId);
                        buf.writeVarInt(msg.count);
                    },
                    buf -> new C2SDuplicate(
                            buf.readVarInt(),
                            Math.max(1, Math.min(64, buf.readVarInt()))
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(C2SDuplicate msg, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ServerPlayer sp = (ServerPlayer) ctx.player();
        ctx.enqueueWork(() -> {
            IJourneyData dat = sp.getCapability(JourneyCapabilities.JOURNEY_DATA, null);
            if (dat == null || !dat.isUnlocked(msg.itemId())) return;

            Item item = BuiltInRegistries.ITEM.byId(msg.itemId());

            int amount = Math.min(Math.max(1, msg.count()), item.getDefaultMaxStackSize());
            if (amount <= 0) return;

            ItemStack give = new ItemStack(item, amount);
            if (!sp.getInventory().add(give)) {
                sp.drop(give, false);
            }
        });
    }
}
