package com.mystic.journeybeyond;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class JourneyMenu extends AbstractContainerMenu {
    private boolean jbSuppressConsume = false;

    private final Container sacrifice = new SimpleContainer(1) {
        @Override public void setChanged() {
            super.setChanged();
            if (!player.level().isClientSide && !jbSuppressConsume) consumeAllForResearch();
        }
    };

    private final Player player;

    public static final int UI_WIDTH = 248;
    public static final int UI_HEIGHT = 256;

    public JourneyMenu(int id, Inventory inv) {
        super(JourneyMenus.JOURNEY_MENU.get(), id);
        this.player = inv.player;

        // 0: sacrifice slot
        this.addSlot(new Slot(sacrifice, 0, 8, 20) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return !stack.isEmpty();
            }
        });

        int invBaseX = (UI_WIDTH - 9 * 18) / 2; // horizontally centered
        int invLabelY = UI_HEIGHT - 96; // space for label above slots
        int invBaseY = invLabelY + 10;  // 10px gap from label to top row

        // Inventory rows
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(inv, x + y * 9 + 9, invBaseX + x * 18, invBaseY + y * 18));
            }
        }
        // Hotbar
        for (int x = 0; x < 9; x++) {
            addSlot(new Slot(inv, x, invBaseX + x * 18, invBaseY + 58));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player p, int index) {
        ItemStack ret = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack in = slot.getItem();
            ret = in.copy();
            if (index == 0) {
                if (!moveItemStackTo(in, 1, 37, true)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(in, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (in.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return ret;
    }

    @Override
    public boolean stillValid(@NotNull Player p) {
        return true;
    }

    private void consumeAllForResearch() {
        ItemStack stack = sacrifice.getItem(0);
        if (stack.isEmpty()) return;

        final int itemId = BuiltInRegistries.ITEM.getId(stack.getItem());
        final int toUse = stack.getCount();

        var dat = player.getCapability(JourneyCapabilities.JOURNEY_DATA, null);
        if (dat == null) return;

        int required = ResearchManager.requiredFor(itemId);
        if (required <= 0) return;

        int left = dat.remaining().getOrDefault(itemId, required);
        int used = Math.min(left, toUse);
        if (used <= 0) return;

        // apply consumption
        stack.shrink(used);
        left -= used;

        // update capability
        if (left <= 0) {
            dat.setRemaining(itemId, -1);
            dat.addUnlocked(itemId);

            // persist to world data exactly once
            var it = BuiltInRegistries.ITEM.byId(itemId);
            var key = BuiltInRegistries.ITEM.getKey(it);
            JourneyWorldData.get(((net.minecraft.server.level.ServerPlayer) player).serverLevel())
                    .addUnlock(player.getUUID(), key); // setDirty() inside

            Networking.broadcastTo((net.minecraft.server.level.ServerPlayer) player);
        } else {
            dat.setRemaining(itemId, left);
            // keep partial progress in world data too
            JourneyWorldData.get(((net.minecraft.server.level.ServerPlayer) player).serverLevel())
                    .saveFromCap(player.getUUID(), dat); // setDirty() inside
        }

        // put remaining stack back without re-triggering another pass
        jbSuppressConsume = true;
        try {
            sacrifice.setItem(0, stack); // updates the slot visuals
        } finally {
            jbSuppressConsume = false;
        }

        broadcastChanges(); // updates the client UI; does not affect persistence
    }
}
