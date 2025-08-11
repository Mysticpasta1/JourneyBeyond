package com.mystic.journeybeyond;

import it.unimi.dsi.fastutil.ints.IntIterators;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

public final class Networking {
    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(JourneyMod.MODID, "main");

    public static void init() {
        PayloadRegistrar registrar = new PayloadRegistrar(JourneyMod.MODID).versioned("1");
        registrar.playBidirectional(C2SDuplicate.TYPE, C2SDuplicate.STREAM_CODEC, C2SDuplicate::handle);
        registrar.playBidirectional(C2SOpenMenu.TYPE, C2SOpenMenu.STREAM_CODEC, C2SOpenMenu::handle);
        registrar.playBidirectional(C2SSetJourney.TYPE, C2SSetJourney.STREAM_CODEC, C2SSetJourney::handle);

        registrar.playToClient(S2CSyncJourney.TYPE, S2CSyncJourney.STREAM_CODEC,
                (msg, ctx) -> S2CSyncJourney.Client.handle(msg));
    }

    public static void syncTo(ServerPlayer sp) {
        var cap = sp.getCapability(JourneyCapabilities.JOURNEY_DATA, null);
        if (cap == null) return;

        // Convert unlocked int IDs -> registry names for the wire
        List<ResourceLocation> names = new ArrayList<>();
        var it = IntIterators.asIntIterator(cap.unlocked().iterator());
        while (it.hasNext()) {
            int id = it.nextInt();
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.byId(id);
            var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            names.add(key);
        }

        PacketDistributor.sendToPlayer(sp, new S2CSyncJourney(names));
    }

    public static void broadcastTo(ServerPlayer source) {
        source.server.getPlayerList().getPlayers().forEach(Networking::syncTo);
    }
}
