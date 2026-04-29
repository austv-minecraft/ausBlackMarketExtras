package plugin.ausBlackMarketingExtras.model;

import java.util.List;

public record CycleConfig(
    int id,
    int startDay,
    int endDay,
    String world,
    int x, int y, int z,
    List<AuctionEntry> auctions,
    BuildingRemovalConfig buildingRemoval,
    HologramConfig hologram
) {}
