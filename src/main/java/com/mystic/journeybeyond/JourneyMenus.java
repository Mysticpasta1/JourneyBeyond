package com.mystic.journeybeyond;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class JourneyMenus {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, JourneyMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<JourneyMenu>> JOURNEY_MENU =
            MENU_TYPES.register("journey_menu",
                () -> new MenuType<>(JourneyMenu::new, FeatureFlagSet.of(FeatureFlags.VANILLA)));

    public static void register(IEventBus bus) {
        MENU_TYPES.register(bus);
    }
}
