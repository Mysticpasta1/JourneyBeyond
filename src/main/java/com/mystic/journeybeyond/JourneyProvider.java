package com.mystic.journeybeyond;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.common.util.INBTSerializable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class JourneyProvider implements
        ICapabilityProvider<Player, @Nullable Void, IJourneyData>,
        INBTSerializable<CompoundTag> {

    private final JourneyData data = new JourneyData();

    @Override
    public IJourneyData getCapability(@NotNull Player player, @Nullable Void ctx) {
        return data;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return data.save(); // saves as names (stable)
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        data.load(nbt); // resolves names -> ids
    }
}
