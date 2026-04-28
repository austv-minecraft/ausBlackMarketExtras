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
    private boolean broadcastEnabled;
    private String broadcastStartMessage;
    private String broadcastEndMessage;
    private String broadcastStartTitle;
    private String broadcastStartSubtitle;
    private String broadcastEndTitle;
    private String broadcastEndSubtitle;
    private int titleFadeIn;
    private int titleStay;
    private int titleFadeOut;
    private String broadcastSound;
    private float soundVolume;
    private float soundPitch;

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

        broadcastEnabled = config.getBoolean("broadcast.enabled", true);
        broadcastStartMessage = config.getString("broadcast.start-message", "");
        broadcastEndMessage = config.getString("broadcast.end-message", "");
        broadcastStartTitle = config.getString("broadcast.start-title", "");
        broadcastStartSubtitle = config.getString("broadcast.start-subtitle", "");
        broadcastEndTitle = config.getString("broadcast.end-title", "");
        broadcastEndSubtitle = config.getString("broadcast.end-subtitle", "");
        titleFadeIn = config.getInt("broadcast.title-fade-in", 10);
        titleStay = config.getInt("broadcast.title-stay", 60);
        titleFadeOut = config.getInt("broadcast.title-fade-out", 20);
        broadcastSound = config.getString("broadcast.sound", "");
        soundVolume = (float) config.getDouble("broadcast.sound-volume", 1.0);
        soundPitch = (float) config.getDouble("broadcast.sound-pitch", 1.0);
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

    public boolean isBroadcastEnabled() { return broadcastEnabled; }
    public String getBroadcastStartMessage() { return broadcastStartMessage; }
    public String getBroadcastEndMessage() { return broadcastEndMessage; }
    public String getBroadcastStartTitle() { return broadcastStartTitle; }
    public String getBroadcastStartSubtitle() { return broadcastStartSubtitle; }
    public String getBroadcastEndTitle() { return broadcastEndTitle; }
    public String getBroadcastEndSubtitle() { return broadcastEndSubtitle; }
    public int getTitleFadeIn() { return titleFadeIn; }
    public int getTitleStay() { return titleStay; }
    public int getTitleFadeOut() { return titleFadeOut; }
    public String getBroadcastSound() { return broadcastSound; }
    public float getSoundVolume() { return soundVolume; }
    public float getSoundPitch() { return soundPitch; }

    public Optional<CycleConfig> findCycleByStartDay(int day) {
        return cycles.stream().filter(c -> c.startDay() == day).findFirst();
    }

    public Optional<CycleConfig> findCycleByEndDay(int day) {
        return cycles.stream().filter(c -> c.endDay() == day).findFirst();
    }
}
