package com.steelstorm.compat.neo.neoforge.event.village;

import com.steelstorm.compat.neo.bus.api.Event;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.List;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;

public class VillagerTradesEvent extends Event {
    private final Int2ObjectMap<List<VillagerTrades.ItemListing>> trades;
    private final VillagerProfession type;
    public VillagerTradesEvent(Int2ObjectMap<List<VillagerTrades.ItemListing>> trades, VillagerProfession type) { this.trades = trades; this.type = type; }
    public Int2ObjectMap<List<VillagerTrades.ItemListing>> getTrades() { return trades; }
    public VillagerProfession getType() { return type; }
}
