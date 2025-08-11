package com.mystic.journeybeyond.mixin;

import com.mystic.journeybeyond.C2SOpenMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Unique private Button journeybeyond$btn;

    @Inject(method = "init", at = @At("TAIL"))
    private void jb$addJourneyButton(CallbackInfo ci) {
        var self = (InventoryScreen)(Object)this;
        int left = ((AbstractContainerScreenAccessor) self).journeybeyond$getLeftPos();
        int x = left + 104 + 24;                 // next to recipe book button
        int y = self.height / 2 - 22;

        journeybeyond$btn = Button.builder(
                Component.translatable("screen.journey.title"),
                b -> PacketDistributor.sendToServer(new C2SOpenMenu())
        ).bounds(x, y, 60, 18).build();

        ((ScreenAccess) self).jb$addRenderableWidget(journeybeyond$btn);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void jb$reposition(GuiGraphics g, int mouseX, int mouseY, float pt, CallbackInfo ci) {
        if (journeybeyond$btn != null) {
            var self = (InventoryScreen)(Object)this;
            int left = ((AbstractContainerScreenAccessor) self).journeybeyond$getLeftPos();
            int x = left + 104 + 24;
            int y = self.height / 2 - 22;
            journeybeyond$btn.setPosition(x, y);
        }
    }
}
