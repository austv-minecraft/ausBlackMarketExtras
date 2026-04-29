package plugin.ausBlackMarketingExtras;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import plugin.ausBlackMarketingExtras.command.AusBlackCommand;
import plugin.ausBlackMarketingExtras.config.ConfigManager;
import plugin.ausBlackMarketingExtras.listener.DarkAuctionsLoadListener;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;
import plugin.ausBlackMarketingExtras.schedule.AuctionScheduler;

import java.util.ArrayList;
import java.util.List;

public final class AusBlackMarketingExtras extends JavaPlugin {

    private ConfigManager configManager;
    private List<BukkitTask> pendingTasks = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        AusCycleLogger.init(getDataFolder().toPath(), getLogger());
        configManager = new ConfigManager(getConfig());
        AusCycleLogger.info("ausBlackMarketingExtras enabled. Awaiting AxDarkAuctions...");
        getServer().getPluginManager().registerEvents(
            new DarkAuctionsLoadListener(this, configManager), this
        );
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
            event.registrar().register("ausblack",
                "Comandos administrativos do ausBlackMarketingExtras",
                List.of(),
                new AusBlackCommand(this))
        );
    }

    @Override
    public void onDisable() {
        AusCycleLogger.info("ausBlackMarketingExtras disabled.");
    }

    public void reloadPlugin() {
        for (BukkitTask task : pendingTasks) {
            task.cancel();
        }
        pendingTasks.clear();
        reloadConfig();
        configManager = new ConfigManager(getConfig());
        pendingTasks = new ArrayList<>(AuctionScheduler.schedule(this, configManager));
        AusCycleLogger.info("ausBlackMarketingExtras reloaded. Scheduled tasks: " + pendingTasks.size());
    }

    public void setPendingTasks(List<BukkitTask> tasks) {
        this.pendingTasks = new ArrayList<>(tasks);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
