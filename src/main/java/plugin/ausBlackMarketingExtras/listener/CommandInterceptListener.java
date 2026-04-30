package plugin.ausBlackMarketingExtras.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import plugin.ausBlackMarketingExtras.config.ConfigManager;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;
import plugin.ausBlackMarketingExtras.model.AuctionEntry;
import plugin.ausBlackMarketingExtras.model.CycleConfig;
import plugin.ausBlackMarketingExtras.model.CycleSnapshot;
import plugin.ausBlackMarketingExtras.persistence.AuctionStateCapture;
import plugin.ausBlackMarketingExtras.persistence.CycleStateRepository;

import java.util.ArrayList;
import java.util.List;

public final class CommandInterceptListener implements Listener {

  private final CycleStateRepository repository;
  private final ConfigManager configManager;

  public CommandInterceptListener(CycleStateRepository repository, ConfigManager configManager) {
    this.repository = repository;
    this.configManager = configManager;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
    if (isAxdaReload(event.getMessage())) {
      saveState();
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onServerCommand(ServerCommandEvent event) {
    if (isAxdaReload(event.getCommand())) {
      saveState();
    }
  }

  private boolean isAxdaReload(String message) {
    String cmd = message.toLowerCase().replaceFirst("^/", "").trim();
    return cmd.startsWith("axda reload") || cmd.startsWith("axdarkauctions reload");
  }

  private void saveState() {
    AusCycleLogger.info("[RESUME] Intercepted /axda reload — saving state before reload.");
    List<CycleSnapshot> snapshots = new ArrayList<>();
    for (CycleConfig cycle : configManager.getCycles()) {
      for (AuctionEntry entry : cycle.auctions()) {
        AuctionStateCapture.capture(cycle.id(), entry.name()).ifPresent(snapshots::add);
      }
    }
    if (!snapshots.isEmpty()) {
      repository.save(snapshots);
      AusCycleLogger.info("[RESUME] Saved state for " + snapshots.size()
          + " auction(s) before /axda reload.");
    }
  }
}
