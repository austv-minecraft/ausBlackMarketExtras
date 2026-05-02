package plugin.ausBlackMarketingExtras.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import plugin.ausBlackMarketingExtras.AusBlackMarketingExtras;
import plugin.ausBlackMarketingExtras.config.ConfigManager;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;
import plugin.ausBlackMarketingExtras.model.AuctionEntry;
import plugin.ausBlackMarketingExtras.model.CycleConfig;
import plugin.ausBlackMarketingExtras.model.CycleSnapshot;
import plugin.ausBlackMarketingExtras.persistence.AuctionStateCapture;
import plugin.ausBlackMarketingExtras.persistence.CycleStateRepository;
import plugin.ausBlackMarketingExtras.resume.AuctionResumeHandler;

import java.util.ArrayList;
import java.util.List;

public final class CommandInterceptListener implements Listener {

  /**
   * Delay in ticks to wait after /axda reload before attempting manual restore.
   * AxDarkAuctionsLoadEvent may not fire on soft reloads; this fallback covers that case.
   * 40 ticks = 2 seconds, enough for axda to finish its internal reload.
   */
  private static final long RESTORE_DELAY_TICKS = 40L;

  private final AusBlackMarketingExtras plugin;
  private final CycleStateRepository repository;
  private final ConfigManager configManager;
  private final AuctionResumeHandler resumeHandler;

  public CommandInterceptListener(
      AusBlackMarketingExtras plugin,
      CycleStateRepository repository,
      ConfigManager configManager,
      AuctionResumeHandler resumeHandler) {
    this.plugin = plugin;
    this.repository = repository;
    this.configManager = configManager;
    this.resumeHandler = resumeHandler;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
    if (isAxdaReload(event.getMessage())) {
      saveAndScheduleRestore();
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onServerCommand(ServerCommandEvent event) {
    if (isAxdaReload(event.getCommand())) {
      saveAndScheduleRestore();
    }
  }

  private boolean isAxdaReload(String message) {
    String cmd = message.toLowerCase().replaceFirst("^/", "").trim();
    return cmd.startsWith("axda reload") || cmd.startsWith("axdarkauctions reload");
  }

  private void saveAndScheduleRestore() {
    // 1. Save current state immediately before axda reloads
    List<CycleSnapshot> snapshots = new ArrayList<>();
    for (CycleConfig cycle : configManager.getCycles()) {
      for (AuctionEntry entry : cycle.auctions()) {
        AuctionStateCapture.capture(cycle.id(), entry.name()).ifPresent(snapshots::add);
      }
    }

    if (snapshots.isEmpty()) {
      AusCycleLogger.info("[RESUME] Intercepted /axda reload — no active auctions to save.");
      return;
    }

    repository.save(snapshots);
    AusCycleLogger.info("[RESUME] Intercepted /axda reload — saved state for "
        + snapshots.size() + " auction(s).");

    // 2. Schedule fallback restore after axda finishes reloading.
    // AxDarkAuctionsLoadEvent may not fire on soft reloads (/axda reload),
    // so this ensures restore happens even if the event is never dispatched.
    // If AxDarkAuctionsLoadEvent DID fire and already restored, repository.hasState()
    // will return false and this task does nothing (idempotent).
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (repository.hasState()) {
        AusCycleLogger.info("[RESUME] AxDarkAuctionsLoadEvent did not fire after /axda reload"
            + " — triggering restore manually.");
        resumeHandler.attemptRestore();
      }
    }, RESTORE_DELAY_TICKS);
  }
}
