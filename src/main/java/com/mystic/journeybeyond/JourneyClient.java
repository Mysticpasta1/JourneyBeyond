package com.mystic.journeybeyond;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = JourneyMod.MODID, value = Dist.CLIENT)
public final class JourneyClient {
    private static final KeyMapping KEY_TOGGLE = new KeyMapping("key.journey.toggle", GLFW.GLFW_KEY_J, "key.categories.inventory");
    private static final KeyMapping KEY_UI = new KeyMapping("key.journey.open", GLFW.GLFW_KEY_K, "key.categories.inventory");

    @SubscribeEvent
    public static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e) {
        e.register(JourneyMenus.JOURNEY_MENU.get(), JourneyScreen::new);
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
        e.register(KEY_TOGGLE);
        e.register(KEY_UI);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (KEY_TOGGLE.consumeClick()) {
            PacketDistributor.sendToServer(new C2SSetJourney(true)); // toggles on/off server-side
        }
        if (KEY_UI.consumeClick()) {
            PacketDistributor.sendToServer(new C2SOpenMenu());
        }
    }
}
