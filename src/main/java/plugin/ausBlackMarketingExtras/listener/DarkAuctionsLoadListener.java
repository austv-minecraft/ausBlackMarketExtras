package plugin.ausBlackMarketingExtras.listener;

import com.artillexstudios.axdarkauctions.api.AxDarkAuctionsLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import plugin.ausBlackMarketingExtras.AusBlackMarketingExtras;
import plugin.ausBlackMarketingExtras.config.ConfigManager;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;
import plugin.ausBlackMarketingExtras.schedule.AuctionScheduler;

public final class DarkAuctionsLoadListener implements Listener {

    private final AusBlackMarketingExtras plugin;
    private final ConfigManager config;

    public DarkAuctionsLoadListener(AusBlackMarketingExtras plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler
    public void onAxDarkAuctionsLoad(AxDarkAuctionsLoadEvent event) {
        AusCycleLogger.info("AxDarkAuctions loaded. Initializing auction scheduler...");
        AuctionScheduler.schedule(plugin, config);
    }
}
