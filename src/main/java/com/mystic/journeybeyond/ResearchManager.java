package com.mystic.journeybeyond;

import com.google.gson.*;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

import java.util.Map;

public class ResearchManager extends SimpleJsonResourceReloadListener {
    public static final String FOLDER = "journey_research";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // item-id (int) -> required count
    private static final Int2IntOpenHashMap REQUIRED = new Int2IntOpenHashMap();

    public ResearchManager() { super(GSON, FOLDER); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> all, ResourceManager rm, ProfilerFiller profiler) {
        REQUIRED.clear();

        // 1) Prefill defaults for EVERY item so everything has a cost
        for (Item item : BuiltInRegistries.ITEM) {
            int id = BuiltInRegistries.ITEM.getId(item);
            REQUIRED.put(id, defaultCost(id, item));
        }

        // 2) Apply datapack overrides (items and tags)
        for (var entry : all.entrySet()) {
            JsonObject root = entry.getValue().getAsJsonObject();
            int count = Math.max(1, root.get("count").getAsInt());

            if (root.has("item")) {
                Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(root.get("item").getAsString()));
                if (it != Items.AIR) {
                    REQUIRED.put(BuiltInRegistries.ITEM.getId(it), count);
                }
            } else if (root.has("tag")) {
                var tagKey = ItemTags.create(ResourceLocation.parse(root.get("tag").getAsString()));
                BuiltInRegistries.ITEM.getTag(tagKey).ifPresent(holders -> {
                    for (Holder<Item> holder : holders) {
                        Item it = holder.value();
                        REQUIRED.put(BuiltInRegistries.ITEM.getId(it), count);
                    }
                });
            }
        }

        // 3) FINAL PASS: reduce costs based on rarity (after overrides)
        for (Item item : BuiltInRegistries.ITEM) {
            int id = BuiltInRegistries.ITEM.getId(item);
            int current = REQUIRED.getOrDefault(id, 1);

            // keep unstackables at 1 regardless of rarity
            if (item.getDefaultMaxStackSize() <= 1) {
                REQUIRED.put(id, 1);
                continue;
            }

            float mult = rarityMultiplier(item.getDefaultInstance().getRarity());
            int reduced = Math.max(1, (int)Math.ceil(current * mult));
            REQUIRED.put(id, reduced);
        }
    }

    /** Get required research count for an item-id (always >= 1 after prefill). */
    public static int requiredFor(int itemId) {
        return REQUIRED.getOrDefault(itemId, 1);
    }

    /** Heuristic default cost for every item (blocks included via BlockItem). */
    private static int defaultCost(int itemId, Item item) {
        int max = item.getDefaultMaxStackSize();
        if (max <= 1) return 1;     // unstackable / damageable
        if (max <= 16) return 8;    // low-stack items
        if (item instanceof BlockItem) return 175; // blocks a bit grindier by default
        return 100;                  // everything else
    }

    /** Rarity → multiplier (lower = easier to research). Tweak to taste. */
    private static float rarityMultiplier(Rarity r) {
        return switch (r) {
            case COMMON    -> 1.00f;
            case UNCOMMON  -> 0.75f;
            case RARE      -> 0.30f;
            case EPIC      -> 0.10f;
        };
    }
}
