package plugin.ausBlackMarketingExtras.model;

public record CycleSnapshot(
    int cycleId,
    String auctionName,
    String state,
    long endTimeEpoch,
    double bid,
    String topBidderUuid,
    String topBidderName,
    int bidPage,
    long savedAt
) {}
