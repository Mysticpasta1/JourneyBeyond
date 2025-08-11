package com.mystic.journeybeyond;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class JourneyWorldData extends SavedData {
    public static final String ID = JourneyMod.MODID + "_journey_world";

    public static void savePlayer(ServerPlayer player) {
        var cap = player.getCapability(JourneyCapabilities.JOURNEY_DATA, null);
        if (cap != null) {
            var data = get(player.serverLevel());
            data.saveFromCap(player.getUUID(), cap);
            data.setDirty();
        }
    }

    public static final SavedData.Factory<JourneyWorldData> FACTORY =
            new SavedData.Factory<>(JourneyWorldData::new, JourneyWorldData::load);

    private final Map<UUID, PlayerData> players = new Object2ObjectOpenHashMap<>();

    public JourneyWorldData() {}

    public static JourneyWorldData load(CompoundTag tag, HolderLookup.Provider provider) {
        JourneyWorldData data = new JourneyWorldData();
        ListTag list = tag.getList("players", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag p = list.getCompound(i);
            UUID id = p.getUUID("uuid");
            PlayerData pd = PlayerData.fromTag(p.getCompound("data"));
            data.players.put(id, pd);
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        ListTag list = new ListTag();
        for (var e : players.entrySet()) {
            CompoundTag p = new CompoundTag();
            p.putUUID("uuid", e.getKey());
            p.put("data", e.getValue().toTag());
            list.add(p);
        }
        tag.put("players", list);
        return tag;
    }

    public static JourneyWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, ID);
    }

    public void loadIntoCap(UUID uuid, IJourneyData cap) {
        PlayerData pd = players.get(uuid);
        if (pd == null || cap == null) return;
        pd.copyInto(cap);
    }

    public void saveFromCap(UUID uuid, IJourneyData cap) {
        if (cap == null) return;
        players.put(uuid, PlayerData.fromCap(cap));
        setDirty();
    }

    public void addUnlock(UUID uuid, ResourceLocation itemName) {
        PlayerData pd = players.computeIfAbsent(uuid, u -> new PlayerData());
        pd.unlocked.add(itemName.toString());
        pd.remaining.remove(itemName.toString());
        setDirty();
    }

    public static final class PlayerData {
        public final Set<String> unlocked = new HashSet<>();
        public final Map<String, Integer> remaining = new HashMap<>();
        public boolean journey = false;

        CompoundTag toTag() {
            CompoundTag t = new CompoundTag();
            ListTag ul = new ListTag();
            for (String s : unlocked) ul.add(StringTag.valueOf(s));
            t.put("unlocked", ul);

            ListTag rem = new ListTag();
            for (var e : remaining.entrySet()) {
                CompoundTag c = new CompoundTag();
                c.putString("id", e.getKey());
                c.putInt("left", e.getValue());
                rem.add(c);
            }
            t.put("remaining", rem);
            t.putBoolean("journey", journey);
            return t;
        }

        static PlayerData fromTag(CompoundTag t) {
            PlayerData pd = new PlayerData();
            if (t.contains("unlocked", 9)) {
                ListTag ul = t.getList("unlocked", 8);
                for (int i = 0; i < ul.size(); i++) pd.unlocked.add(ul.getString(i));
            }
            if (t.contains("remaining", 9)) {
                ListTag rl = t.getList("remaining", 10);
                for (int i = 0; i < rl.size(); i++) {
                    CompoundTag c = rl.getCompound(i);
                    pd.remaining.put(c.getString("id"), c.getInt("left"));
                }
            }
            pd.journey = t.getBoolean("journey");
            return pd;
        }

        void copyInto(IJourneyData cap) {
            cap.unlocked().clear();
            cap.remaining().clear();
            for (String s : unlocked) {
                var rl = ResourceLocation.parse(s);
                var it = BuiltInRegistries.ITEM.get(rl);
                int id = BuiltInRegistries.ITEM.getId(it);
                if (id >= 0) cap.unlocked().add(id);
            }

            for (var e : remaining.entrySet()) {
                var rl = ResourceLocation.parse(e.getKey());
                var it = BuiltInRegistries.ITEM.get(rl);
                int id = BuiltInRegistries.ITEM.getId(it);
                if (id < 0) continue;
                int left = e.getValue() != null ? e.getValue() : 0;
                if (left > 0) {
                    cap.remaining().put(id, left);
                } else {
                    cap.setRemaining(id, -1);
                }
            }

            cap.setJourney(journey);
        }


        static PlayerData fromCap(IJourneyData cap) {
            PlayerData pd = new PlayerData();
            cap.unlocked().forEach((int id) -> {
                var it = BuiltInRegistries.ITEM.byId(id);
                var key = BuiltInRegistries.ITEM.getKey(it);
                pd.unlocked.add(key.toString());
            });
            cap.remaining().int2IntEntrySet().forEach(e -> {
                var it = BuiltInRegistries.ITEM.byId(e.getIntKey());
                var key = BuiltInRegistries.ITEM.getKey(it);
                pd.remaining.put(key.toString(), e.getIntValue());
            });
            pd.journey = cap.isJourney();
            return pd;
        }
    }
}
