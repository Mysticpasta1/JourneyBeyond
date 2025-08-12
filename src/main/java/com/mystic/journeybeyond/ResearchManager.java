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

/**
 * Loads research costs from data packs in data/<namespace>/journey_research/*.json
 * Format examples:
 * { "item": "minecraft:diamond", "count": 8 }
 * { "tag": "minecraft:planks", "count": 64 }
 *
 * Defaults are prefilled for every registry item; datapack entries override.
 * Final pass adjusts by rarity and special rules:
 * - Unstackables → 1
 * - EPIC → 1   (requested)
 */
public class ResearchManager extends SimpleJsonResourceReloadListener {
    public static final String FOLDER = "journey_research";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // item-id (int) -> required count
    private static final Int2IntOpenHashMap REQUIRED = new Int2IntOpenHashMap();

    public ResearchManager() {
        super(GSON, FOLDER);
    }

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
            JsonElement elem = entry.getValue();
            if (elem == null || !elem.isJsonObject()) continue;

            JsonObject root = elem.getAsJsonObject();
            int count = readCountSafe(root);

            if (root.has("item") && root.get("item").isJsonPrimitive()) {
                String itemStr = root.get("item").getAsString();
                Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemStr));
                if (it != null && it != Items.AIR) {
                    int id = BuiltInRegistries.ITEM.getId(it);
                    if (id >= 0) REQUIRED.put(id, count);
                }
            } else if (root.has("tag") && root.get("tag").isJsonPrimitive()) {
                String tagStr = root.get("tag").getAsString();
                var tagKey = ItemTags.create(ResourceLocation.parse(tagStr));
                BuiltInRegistries.ITEM.getTag(tagKey).ifPresent(holders -> {
                    for (Holder<Item> holder : holders) {
                        Item it = holder.value();
                        if (it == Items.AIR) continue;
                        int id = BuiltInRegistries.ITEM.getId(it);
                        if (id >= 0) REQUIRED.put(id, count);
                    }
                });
            }
        }

        // 3) FINAL PASS: apply rarity rules after overrides
        for (Item item : BuiltInRegistries.ITEM) {
            int id = BuiltInRegistries.ITEM.getId(item);
            if (id < 0) continue;

            // Always skip AIR / make it 1 (or skip entirely)
            if (item == Items.AIR) {
                REQUIRED.put(id, 1);
                continue;
            }

            // keep unstackables at 1 regardless of rarity
            if (item.getDefaultMaxStackSize() <= 1) {
                REQUIRED.put(id, 1);
                continue;
            }

            // EPIC items must be exactly 1
            Rarity rarity = safeRarity(item);
            if (rarity == Rarity.EPIC) {
                REQUIRED.put(id, 1);
                continue;
            }

            int current = REQUIRED.getOrDefault(id, 1);
            float mult = rarityMultiplier(rarity);
            int reduced = Math.max(1, (int) Math.ceil(current * mult));
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
        if (max <= 1) return 1;      // unstackable / damageable
        if (max <= 16) return 8;     // low-stack items
        if (item instanceof BlockItem) return 175; // blocks a bit grindier by default
        return 100;                  // everything else
    }

    /** Null-safe rarity read that avoids MatchException callers. */
    private static Rarity safeRarity(Item item) {
        try {
            var stack = item.getDefaultInstance();
            return stack.getRarity();
        } catch (Throwable t) {
            // Be defensive against odd modded overrides
            return Rarity.COMMON;
        }
    }

    /** Rarity → multiplier (lower = easier to research). EPIC handled earlier as exactly 1. */
    private static float rarityMultiplier(Rarity r) {
        if (r == null) return 1.00f; // treat unknowns as COMMON
        return switch (r) {
            case COMMON    -> 1.00f;
            case UNCOMMON  -> 0.75f;
            case RARE      -> 0.30f;
            default -> 1.00f; // won't be used; EPIC is forced to 1 above
        };
    }

    /** Robust reader for "count" that tolerates missing/wrong types. */
    private static int readCountSafe(JsonObject root) {
        if (root.has("count") && root.get("count").isJsonPrimitive()) {
            try {
                int v = root.get("count").getAsInt();
                return Math.max(1, v);
            } catch (Exception ignored) {}
        }
        return 1;
    }
}
