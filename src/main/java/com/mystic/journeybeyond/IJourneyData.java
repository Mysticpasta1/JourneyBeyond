// src/main/java/com/example/journey/state/IJourneyData.java
package com.mystic.journeybeyond;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntSet;

public interface IJourneyData {
    IntSet unlocked();              // item ids fully researched
    Int2IntMap remaining();         // item id -> count left to sacrifice
    default boolean isUnlocked(int itemId) { return unlocked().contains(itemId); }
    default int remainingFor(int itemId) { return remaining().getOrDefault(itemId, -1); }
    void setRemaining(int itemId, int left); // -1 to clear
    void addUnlocked(int itemId);
    void copyFrom(IJourneyData other);
    boolean isJourney();
    void setJourney(boolean v);
}
