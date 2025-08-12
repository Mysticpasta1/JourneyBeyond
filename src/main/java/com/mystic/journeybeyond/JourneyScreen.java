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
    private static final int ITEMS_PER_PAGE = COLS * ROWS;

    // Paging
    private int page = 0;

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
                    return it != null && (q.isEmpty() ||
                            BuiltInRegistries.ITEM.getKey(it).toString().toLowerCase(Locale.ROOT).contains(q));
                })
                .boxed().collect(Collectors.toList());
        page = 0; // reset to first page on new filter
    }

    private int totalPages() {
        if (filteredIds == null || filteredIds.isEmpty()) return 1;
        return Math.max(1, (filteredIds.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
    }

    private void clampPage() {
        int max = totalPages() - 1;
        if (page < 0) page = 0;
        if (page > max) page = max;
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

        // Page indicator (right of "Duplication")
        String pageText = "Page " + (page + 1) + "/" + totalPages();
        int pageTextX = leftPos + imageWidth - 8 - font.width(pageText);
        g.drawString(font, pageText, pageTextX, topPos + 28, TEXT_COLOR, false);

        // Sacrifice slot background
        g.fill(leftPos + 8, topPos + 20, leftPos + 24, topPos + 36, SACRIFICE_BG);

        // Duplication grid
        int startX = leftPos + 60;
        int startY = topPos + 44;

        int startIndex = page * ITEMS_PER_PAGE;
        int remaining = Math.max(0, (filteredIds == null ? 0 : filteredIds.size()) - startIndex);
        int toShow = Math.min(ITEMS_PER_PAGE, remaining);

        // Grid tiles
        for (int i = 0; i < toShow; i++) {
            int x = startX + (i % COLS) * CELL;
            int y = startY + (i / COLS) * CELL;
            g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_BG);
        }

        // Items + hover
        for (int i = 0; i < toShow; i++) {
            int idx = startIndex + i;
            int x = startX + (i % COLS) * CELL;
            int y = startY + (i / COLS) * CELL;

            Item it = BuiltInRegistries.ITEM.byId(filteredIds.get(idx));
            if (it == null) continue;
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
        // Let the search box handle clicks first
        if (this.search != null && this.search.mouseClicked(mx, my, btn)) {
            setFocused(this.search);
            return true;
        }

        int startX = leftPos + 60;
        int startY = topPos + 44;

        int startIndex = page * ITEMS_PER_PAGE;
        int remaining = Math.max(0, (filteredIds == null ? 0 : filteredIds.size()) - startIndex);
        int toShow = Math.min(ITEMS_PER_PAGE, remaining);

        for (int i = 0; i < toShow; i++) {
            int x = startX + (i % COLS) * CELL;
            int y = startY + (i / COLS) * CELL;
            if (mx >= x && mx < x + 16 && my >= y && my < y + 16) {
                int id = filteredIds.get(startIndex + i);

                // Left click = 1, Right click = full stack
                int request;
                if (btn == 0) {
                    request = 1;
                } else if (btn == 1) {
                    Item it = BuiltInRegistries.ITEM.byId(id);
                    if (it == null) return true;
                    request = Math.max(1, it.getDefaultMaxStackSize());
                } else {
                    return true;
                }

                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new C2SDuplicate(id, request));
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        // dy > 0 => scroll up (previous page), dy < 0 => next page
        if (filteredIds != null && filteredIds.size() > ITEMS_PER_PAGE) {
            page += (dy < 0) ? 1 : -1;
            clampPage();
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // PageUp/PageDown support
        // 201 = GLFW_KEY_PAGE_UP, 209 = GLFW_KEY_PAGE_DOWN in LWJGL key codes
        if (keyCode == 201) { // PgUp
            page--;
            clampPage();
            return true;
        } else if (keyCode == 209) { // PgDn
            page++;
            clampPage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mx, int my) { }
}
