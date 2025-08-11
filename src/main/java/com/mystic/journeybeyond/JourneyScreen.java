package com.mystic.journeybeyond;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class JourneyScreen extends AbstractContainerScreen<JourneyMenu> {
    private EditBox search;
    private List<Integer> filteredIds;

    // Match menu
    private static final int UI_WIDTH  = JourneyMenu.UI_WIDTH;
    private static final int UI_HEIGHT = JourneyMenu.UI_HEIGHT;

    // Colors
    private static final int PANEL_BG         = 0xCC1B1D21;
    private static final int TEXT_COLOR       = 0xFFE0E5EC;
    private static final int SLOT_BG          = 0xFF2E3139;
    private static final int SLOT_BG_HOVER    = 0x66FFFFFF;
    private static final int SACRIFICE_BG     = 0xFF3B2A2A;
    private static final int INV_SLOT_BG      = 0xFF2B3A3A;
    private static final int HOTBAR_SLOT_BG   = 0xFF3A2B3A;

    // Duplication grid
    private static final int COLS = 8;
    private static final int ROWS = 6;
    private static final int CELL = 18;

    public JourneyScreen(JourneyMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = UI_WIDTH;
        this.imageHeight = UI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int searchX = leftPos + 60;
        int searchY = topPos + 10;
        int searchW = Math.max(150, imageWidth - 60 - 12);
        search = new EditBox(font, searchX, searchY, searchW, 16, Component.literal("Search"));
        search.setBordered(true);
        search.setValue("");
        addRenderableWidget(search);
        refilter();
    }

    private void refilter() {
        String q = search.getValue().trim().toLowerCase(Locale.ROOT);
        var unlocked = S2CSyncJourney.Client.UNLOCKED;
        filteredIds = unlocked.intStream()
                .filter(id -> {
                    Item it = BuiltInRegistries.ITEM.byId(id);
                    return q.isEmpty() || BuiltInRegistries.ITEM.getKey(it).toString().toLowerCase(Locale.ROOT).contains(q);
                })
                .boxed().collect(Collectors.toList());
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (search.isFocused()) refilter();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_BG);

        // Titles
        g.drawString(font, "Sacrifice", leftPos + 8, topPos + 8, TEXT_COLOR, false);
        g.drawString(font, "Duplication", leftPos + 60, topPos + 28, TEXT_COLOR, false);

        // Sacrifice slot background
        g.fill(leftPos + 8, topPos + 20, leftPos + 24, topPos + 36, SACRIFICE_BG);

        // Duplication grid
        int startX = leftPos + 60;
        int startY = topPos + 44;
        int maxIcons = Math.min(filteredIds.size(), COLS * ROWS);
        for (int i = 0; i < maxIcons; i++) {
            int x = startX + (i % COLS) * CELL;
            int y = startY + (i / COLS) * CELL;
            g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_BG);
        }
        for (int i = 0; i < maxIcons; i++) {
            int x = startX + (i % COLS) * CELL;
            int y = startY + (i / COLS) * CELL;
            Item it = BuiltInRegistries.ITEM.byId(filteredIds.get(i));
            ItemStack stack = it.getDefaultInstance();
            g.renderItem(stack, x, y);
            if (isHovering(x - leftPos, y - topPos, 16, 16, mx, my)) {
                g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_BG_HOVER);
                g.renderTooltip(this.font, stack, mx, my);
            }
        }

        // Inventory label position
        int invBaseX = leftPos + (imageWidth - 9 * 18) / 2;
        int invLabelY = topPos + imageHeight - 96; // where label is drawn
        int invBaseY = invLabelY + 10;             // slot area below label

        // Draw label centered
        int labelX = invBaseX + (9 * 18 - font.width(this.playerInventoryTitle)) / 2;
        g.drawString(font, this.playerInventoryTitle, labelX, invLabelY, TEXT_COLOR, false);

        // Inventory + hotbar backgrounds
        for (Slot slot : this.menu.slots) {
            if (slot.index == 0) continue; // skip sacrifice
            int sx = leftPos + slot.x;
            int sy = topPos + slot.y;
            if (slot.index >= 1 && slot.index < 28) {
                g.fill(sx, sy, sx + 16, sy + 16, INV_SLOT_BG);
            } else if (slot.index >= 28) {
                g.fill(sx, sy, sx + 16, sy + 16, HOTBAR_SLOT_BG);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int startX = leftPos + 60;
        int startY = topPos + 44;
        int maxIcons = Math.min(filteredIds.size(), COLS * ROWS);
        for (int i = 0; i < maxIcons; i++) {
            int x = startX + (i % COLS) * CELL;
            int y = startY + (i / COLS) * CELL;
            if (mx >= x && mx < x + 16 && my >= y && my < y + 16) {
                int id = filteredIds.get(i);
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new C2SDuplicate(id, 64));
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mx, int my) { }
}
