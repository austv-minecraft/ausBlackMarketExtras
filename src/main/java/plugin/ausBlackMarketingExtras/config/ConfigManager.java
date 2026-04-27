package plugin.ausBlackMarketingExtras.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import plugin.ausBlackMarketingExtras.discord.EmbedConfig;
import plugin.ausBlackMarketingExtras.model.AuctionEntry;
import plugin.ausBlackMarketingExtras.model.CycleConfig;

import java.util.*;

public final class ConfigManager {

    private final List<CycleConfig> cycles = new ArrayList<>();
    private int triggerHour;
    private int triggerMinute;
    private String schematic;
    private boolean discordEnabled;
    private String webhookUrl;
    private EmbedConfig startEmbed;
    private EmbedConfig endEmbed;

    public ConfigManager(FileConfiguration config) {
        schematic = config.getString("schematic", "dark_auction.schem");
        triggerHour = config.getInt("trigger-hour", 12);
        triggerMinute = config.getInt("trigger-minute", 0);

        ConfigurationSection cyclesSection = config.getConfigurationSection("cycles");
        if (cyclesSection != null) {
            for (String key : cyclesSection.getKeys(false)) {
                ConfigurationSection cs = cyclesSection.getConfigurationSection(key);
                if (cs == null) continue;
                int id = Integer.parseInt(key);
                int startDay = cs.getInt("start-day");
                int endDay = cs.getInt("end-day");
                ConfigurationSection coord = cs.getConfigurationSection("coordinate");
                String world = coord != null ? coord.getString("world", "world") : "world";
                int x = coord != null ? coord.getInt("x", 0) : 0;
                int y = coord != null ? coord.getInt("y", 64) : 64;
                int z = coord != null ? coord.getInt("z", 0) : 0;
                List<AuctionEntry> entries = new ArrayList<>();
                for (Map<?, ?> map : cs.getMapList("auctions")) {
                    entries.add(new AuctionEntry(
                        (String) map.get("name"),
                        (int) map.get("npc-id")
                    ));
                }
                cycles.add(new CycleConfig(id, startDay, endDay, world, x, y, z, Collections.unmodifiableList(entries)));
            }
        }

        discordEnabled = config.getBoolean("discord.enabled", false);
        webhookUrl = config.getString("discord.webhook-url", "");

        ConfigurationSection startSection = config.getConfigurationSection("discord.start-embed");
        startEmbed = startSection != null ? EmbedConfig.fromSection(startSection) : EmbedConfig.empty();

        ConfigurationSection endSection = config.getConfigurationSection("discord.end-embed");
        endEmbed = endSection != null ? EmbedConfig.fromSection(endSection) : EmbedConfig.empty();
    }

    public List<CycleConfig> getCycles() {
        return Collections.unmodifiableList(cycles);
    }

    public int getTriggerHour() { return triggerHour; }
    public int getTriggerMinute() { return triggerMinute; }
    public String getSchematic() { return schematic; }
    public boolean isDiscordEnabled() { return discordEnabled; }
    public String getWebhookUrl() { return webhookUrl; }
    public EmbedConfig getStartEmbed() { return startEmbed; }
    public EmbedConfig getEndEmbed() { return endEmbed; }

    public Optional<CycleConfig> findCycleByStartDay(int day) {
        return cycles.stream().filter(c -> c.startDay() == day).findFirst();
    }

    public Optional<CycleConfig> findCycleByEndDay(int day) {
        return cycles.stream().filter(c -> c.endDay() == day).findFirst();
    }
}
