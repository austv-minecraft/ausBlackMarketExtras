package plugin.ausBlackMarketingExtras.model;

public record BuildingRemovalConfig(
    String world,
    int x1, int y1, int z1,
    int x2, int y2, int z2
) {}
