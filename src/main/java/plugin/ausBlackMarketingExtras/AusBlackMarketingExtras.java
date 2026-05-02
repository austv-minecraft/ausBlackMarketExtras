package plugin.ausBlackMarketingExtras;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import plugin.ausBlackMarketingExtras.auction.AuctionHandler;
import plugin.ausBlackMarketingExtras.command.AusBlackCommand;
import plugin.ausBlackMarketingExtras.config.ConfigManager;
import plugin.ausBlackMarketingExtras.listener.CommandInterceptListener;
import plugin.ausBlackMarketingExtras.listener.DarkAuctionsLoadListener;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;
import plugin.ausBlackMarketingExtras.persistence.CycleStateRepository;
import plugin.ausBlackMarketingExtras.resume.AuctionResumeHandler;
import plugin.ausBlackMarketingExtras.schedule.AuctionScheduler;

import java.util.ArrayList;
import java.util.List;

public final class AusBlackMarketingExtras extends JavaPlugin {

  private ConfigManager configManager;
  private List<BukkitTask> pendingTasks = new ArrayList<>();
  private CycleStateRepository stateRepository;
  private AuctionResumeHandler resumeHandler;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    AusCycleLogger.init(getDataFolder().toPath(), getLogger());
    configManager = new ConfigManager(getConfig());

    stateRepository = new CycleStateRepository(this);
    AuctionHandler.init(stateRepository);
    resumeHandler = new AuctionResumeHandler(this, stateRepository, configManager);

    AusCycleLogger.info("ausBlackMarketingExtras enabled. Awaiting AxDarkAuctions...");

    getServer().getPluginManager().registerEvents(
        new CommandInterceptListener(this, stateRepository, configManager, resumeHandler), this
    );
    getServer().getPluginManager().registerEvents(
        new DarkAuctionsLoadListener(this, configManager, resumeHandler), this
    );
    getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
        event.registrar().register(
            "ausblack",
            "Comandos administrativos do ausBlackMarketingExtras",
            List.of(),
            new AusBlackCommand(this, stateRepository))
    );

    // Fallback restore for server restart: if saved state exists but
    // AxDarkAuctionsLoadEvent never fires (e.g. event dispatched before our
    // listener was registered), this task triggers restore after 60 ticks (3s),
    // by which time all plugins and worlds are fully ready.
    // Idempotent: if the event already fired and cleared the state, hasState()
    // returns false and this task does nothing.
    if (stateRepository.hasState()) {
      getServer().getScheduler().runTaskLater(this, () -> {
        if (stateRepository.hasState()) {
          AusCycleLogger.info("[RESUME] Startup fallback: AxDarkAuctionsLoadEvent did not fire"
              + " — triggering restore manually.");
          resumeHandler.attemptRestore();
        }
      }, 60L);
    }
  }

  @Override
  public void onDisable() {
    AuctionHandler.saveAllActiveCycles(configManager);
    AusCycleLogger.info("ausBlackMarketingExtras disabled.");
  }

  public void reloadPlugin() {
    AuctionHandler.saveAllActiveCycles(configManager);
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

  public CycleStateRepository getStateRepository() {
    return stateRepository;
  }
}
