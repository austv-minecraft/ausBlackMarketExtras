package plugin.ausBlackMarketingExtras;

import org.bukkit.plugin.java.JavaPlugin;
import plugin.ausBlackMarketingExtras.config.ConfigManager;
import plugin.ausBlackMarketingExtras.listener.DarkAuctionsLoadListener;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;

public final class AusBlackMarketingExtras extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        AusCycleLogger.init(getDataFolder().toPath(), getLogger());
        ConfigManager config = new ConfigManager(getConfig());
        AusCycleLogger.info("ausBlackMarketingExtras enabled. Awaiting AxDarkAuctions...");
        getServer().getPluginManager().registerEvents(
            new DarkAuctionsLoadListener(this, config), this
        );
    }

    @Override
    public void onDisable() {
        AusCycleLogger.info("ausBlackMarketingExtras disabled.");
    }
}
