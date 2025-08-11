package com.mystic.journeybeyond;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod(JourneyMod.MODID)
public class JourneyMod {
    public static final String MODID = "journeybeyond";

    public JourneyMod(IEventBus modBus) {
        JourneyMenus.register(modBus);
        JourneyCapabilities.register(modBus);
        Networking.init();

        NeoForge.EVENT_BUS.addListener(this::onReloadListeners);
        NeoForge.EVENT_BUS.addListener(this::onClone);
    }

    private void onReloadListeners(AddReloadListenerEvent e) {
        e.addListener(new ResearchManager());
    }

    private void onClone(PlayerEvent.Clone e) {
        if (e.getEntity().level().isClientSide()) return;

        IJourneyData oldCap = e.getOriginal().getCapability(JourneyCapabilities.JOURNEY_DATA, null);
        IJourneyData newCap = e.getEntity().getCapability(JourneyCapabilities.JOURNEY_DATA, null);
        if (oldCap != null && newCap != null) {
            newCap.copyFrom(oldCap);
            Networking.syncTo((ServerPlayer) e.getEntity());
        }
    }
}
