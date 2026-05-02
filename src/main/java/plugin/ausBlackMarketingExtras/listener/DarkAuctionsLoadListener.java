package plugin.ausBlackMarketingExtras.listener;

import com.artillexstudios.axdarkauctions.api.AxDarkAuctionsLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import plugin.ausBlackMarketingExtras.AusBlackMarketingExtras;
import plugin.ausBlackMarketingExtras.config.ConfigManager;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;
import plugin.ausBlackMarketingExtras.resume.AuctionResumeHandler;
import plugin.ausBlackMarketingExtras.schedule.AuctionScheduler;

import java.util.List;

public final class DarkAuctionsLoadListener implements Listener {

  private final AusBlackMarketingExtras plugin;
  private final ConfigManager config;
  private final AuctionResumeHandler resumeHandler;

  public DarkAuctionsLoadListener(
      AusBlackMarketingExtras plugin,
      ConfigManager config,
      AuctionResumeHandler resumeHandler) {
    this.plugin = plugin;
    this.config = config;
    this.resumeHandler = resumeHandler;
  }

  @EventHandler
  public void onAxDarkAuctionsLoad(AxDarkAuctionsLoadEvent event) {
    AusCycleLogger.info("AxDarkAuctions loaded. Checking for saved state...");
    boolean restored = resumeHandler.attemptRestore();
    if (!restored) {
      AusCycleLogger.info("No saved state. Initializing auction scheduler...");
      List<BukkitTask> tasks = AuctionScheduler.schedule(plugin, config);
      plugin.setPendingTasks(tasks);
    }
  }
}
