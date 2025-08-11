package com.mystic.journeybeyond;

import com.mojang.brigadier.Command;

import static net.minecraft.commands.Commands.*;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

@EventBusSubscriber(modid = JourneyMod.MODID)
public final class JourneyCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        var d = e.getDispatcher();

        d.register(literal("journey")
                .then(literal("on").executes(ctx -> {
                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                    IJourneyData dat = sp.getCapability(JourneyCapabilities.JOURNEY_DATA, null);
                    if (dat != null) {
                        dat.setJourney(true);
                        sp.displayClientMessage(Component.literal("Journey Mode: ON"), true);
                        sp.onUpdateAbilities();
                        Networking.syncTo(sp);
                    }
                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("off").executes(ctx -> {
                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                    IJourneyData dat = sp.getCapability(JourneyCapabilities.JOURNEY_DATA, null);
                    if (dat != null) {
                        dat.setJourney(false);
                        sp.displayClientMessage(Component.literal("Journey Mode: OFF"), true);
                        sp.onUpdateAbilities();
                        Networking.syncTo(sp);
                    }
                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("ui").executes(ctx -> {
                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                    sp.openMenu(new net.minecraft.world.SimpleMenuProvider(
                            (id, inv, p) -> new JourneyMenu(id, inv),
                            net.minecraft.network.chat.Component.translatable("screen.journey.title")
                    ));
                    return 1;
                }))
        );
    }
}
