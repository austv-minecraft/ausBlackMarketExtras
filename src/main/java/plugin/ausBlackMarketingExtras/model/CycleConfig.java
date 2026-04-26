package plugin.ausBlackMarketingExtras.model;

import java.util.List;

public record CycleConfig(int id, int startDay, int endDay, List<AuctionEntry> auctions) {}
