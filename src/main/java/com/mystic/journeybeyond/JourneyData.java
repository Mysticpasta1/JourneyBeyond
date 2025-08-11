package com.mystic.journeybeyond;

import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class JourneyData implements IJourneyData {
    private final IntOpenHashSet unlocked = new IntOpenHashSet();
    private final Int2IntOpenHashMap remaining = new Int2IntOpenHashMap();
    private boolean journey;

    @Override public IntSet unlocked() { return unlocked; }
    @Override public Int2IntMap remaining() { return remaining; }
    @Override public void setRemaining(int itemId, int left) { if (left <= 0) remaining.remove(itemId); else remaining.put(itemId, left); }
    @Override public void addUnlocked(int itemId) { unlocked.add(itemId); }
    @Override public boolean isJourney() { return journey; }
    @Override public void setJourney(boolean v) { journey = v; }

    @Override public void copyFrom(IJourneyData other) {
        unlocked.clear(); unlocked.addAll(other.unlocked());
        remaining.clear(); remaining.putAll(other.remaining());
        journey = other.isJourney();
    }

    /** Save AS NAMES (stable) */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        // unlocked -> list of item names
        ListTag ul = new ListTag();
        for (int id : unlocked) {
            Item it = BuiltInRegistries.ITEM.byId(id);
            if (it == null) continue;
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(it);
            if (key != null) ul.add(StringTag.valueOf(key.toString()));
        }
        tag.put("unlocked", ul);

        // remaining -> list of { id:"ns:path", left:int }
        ListTag rem = new ListTag();
        remaining.int2IntEntrySet().forEach(e -> {
            Item it = BuiltInRegistries.ITEM.byId(e.getIntKey());
            if (it == null) return;
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(it);
            if (key == null) return;
            CompoundTag t = new CompoundTag();
            t.putString("id", key.toString());
            t.putInt("left", e.getIntValue());
            rem.add(t);
        });
        tag.put("remaining", rem);

        tag.putBoolean("journey", journey);
        return tag;
    }

    /** Load from names; migrate legacy int-array if present */
    public void load(CompoundTag tag) {
        unlocked.clear(); remaining.clear();

        // MIGRATE: legacy int-array
        if (tag.contains("unlocked", 11)) {
            for (int raw : tag.getIntArray("unlocked")) {
                Item it = BuiltInRegistries.ITEM.byId(raw);
                if (it != null) unlocked.add(BuiltInRegistries.ITEM.getId(it));
            }
        }

        // unlocked (names)
        if (tag.contains("unlocked", 9)) {
            ListTag list = tag.getList("unlocked", 8);
            for (int i=0;i<list.size();i++) {
                try {
                    ResourceLocation key = ResourceLocation.parse(list.getString(i));
                    Item it = BuiltInRegistries.ITEM.get(key);
                    if (it != null) unlocked.add(BuiltInRegistries.ITEM.getId(it));
                } catch (Exception ignored) {}
            }
        }

        // remaining (names)
        if (tag.contains("remaining", 9)) {
            ListTag list = tag.getList("remaining", 10);
            for (int i=0;i<list.size();i++) {
                CompoundTag t = list.getCompound(i);
                try {
                    ResourceLocation key = ResourceLocation.parse(t.getString("id"));
                    int left = t.getInt("left");
                    Item it = BuiltInRegistries.ITEM.get(key);
                    if (it != null) remaining.put(BuiltInRegistries.ITEM.getId(it), left);
                } catch (Exception ignored) {}
            }
        }

        journey = tag.getBoolean("journey");
    }
}
