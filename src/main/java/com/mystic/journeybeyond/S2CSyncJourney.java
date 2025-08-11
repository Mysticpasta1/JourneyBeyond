package com.mystic.journeybeyond;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record S2CSyncJourney(List<ResourceLocation> itemNames) implements CustomPacketPayload {
    public static final Type<S2CSyncJourney> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JourneyMod.MODID, "sync_journey"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSyncJourney> STREAM_CODEC =
            CustomPacketPayload.codec(
                    (msg, buf) -> {
                        buf.writeVarInt(msg.itemNames.size());
                        for (ResourceLocation rl : msg.itemNames) buf.writeResourceLocation(rl);
                    },
                    buf -> {
                        int n = buf.readVarInt();
                        List<ResourceLocation> names = new ArrayList<>(n);
                        for (int i=0;i<n;i++) names.add(buf.readResourceLocation());
                        return new S2CSyncJourney(names);
                    }
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Client cache of unlocked item *ids* (rebuilt from names on receive) */
    public static final class Client {
        public static final IntArrayList UNLOCKED = new IntArrayList();

        public static void handle(S2CSyncJourney msg) {
            UNLOCKED.clear();
            for (ResourceLocation rl : msg.itemNames) {
                var it = BuiltInRegistries.ITEM.get(rl);
                if (it != null) {
                    int id = BuiltInRegistries.ITEM.getId(it);
                    if (id >= 0) UNLOCKED.add(id);
                }
            }
        }
    }
}
