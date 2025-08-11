package com.mystic.journeybeyond;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = JourneyMod.MODID)
public final class JourneyCapabilities {

    public static final ResourceLocation KEY =
            ResourceLocation.fromNamespaceAndPath(JourneyMod.MODID, "journey");

    public static final EntityCapability<IJourneyData, Void> JOURNEY_DATA =
            EntityCapability.createVoid(KEY, IJourneyData.class);

    public static void register(IEventBus modBus) {
        modBus.addListener(JourneyCapabilities::onRegisterCaps);
    }

    private static void onRegisterCaps(RegisterCapabilitiesEvent event) {
        event.registerEntity(
                JOURNEY_DATA,
                EntityType.PLAYER,
                new JourneyProvider()
        );
    }

    @SubscribeEvent
    public static void onLogin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp)) return;

        // Pull from world file -> cap
        var cap = sp.getCapability(JOURNEY_DATA, null);
        if (cap != null) {
            JourneyWorldData.get(sp.serverLevel()).loadIntoCap(sp.getUUID(), cap);
        }

        Networking.syncTo(sp);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity().level().isClientSide() || !(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
        Networking.syncTo(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            JourneyWorldData.savePlayer(sp);
        }
    }
}
