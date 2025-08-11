package com.mystic.journeybeyond;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = JourneyMod.MODID)
public final class JourneyAbilities {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (e.getEntity().level().isClientSide()) return;
        ServerPlayer sp = (ServerPlayer) e.getEntity();

        IJourneyData dat = sp.getCapability(JourneyCapabilities.JOURNEY_DATA, null);
        if (dat == null) return;

        var ab = sp.getAbilities();
        if (dat.isJourney()) {
            // “Journey Mode” feel: survival rules + flight convenience
            if (!ab.mayfly) { ab.mayfly = true; sp.onUpdateAbilities(); }
        } else {
            if (ab.mayfly && !sp.isCreative()) { // don’t nerf actual creative
                ab.mayfly = false;
                ab.flying = false;
                sp.onUpdateAbilities();
            }
        }
    }
}
