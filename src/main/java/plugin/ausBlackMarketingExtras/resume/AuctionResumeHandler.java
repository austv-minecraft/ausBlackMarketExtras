package plugin.ausBlackMarketingExtras.resume;

import com.artillexstudios.axdarkauctions.auctions.Auction;
import com.artillexstudios.axdarkauctions.auctions.AuctionManager;
import com.artillexstudios.axdarkauctions.enums.State;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import plugin.ausBlackMarketingExtras.AusBlackMarketingExtras;
import plugin.ausBlackMarketingExtras.auction.AuctionHandler;
import plugin.ausBlackMarketingExtras.config.ConfigManager;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;
import plugin.ausBlackMarketingExtras.model.CycleConfig;
import plugin.ausBlackMarketingExtras.model.CycleSnapshot;
import plugin.ausBlackMarketingExtras.persistence.CycleStateRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class AuctionResumeHandler {

  private final AusBlackMarketingExtras plugin;
  private final CycleStateRepository repository;
  private final ConfigManager configManager;

  public AuctionResumeHandler(
      AusBlackMarketingExtras plugin,
      CycleStateRepository repository,
      ConfigManager configManager) {
    this.plugin = plugin;
    this.repository = repository;
    this.configManager = configManager;
  }

  /**
   * Attempts to restore saved auction state.
   *
   * @return true if restoration was performed (skip normal scheduler), false to fall back
   */
  public boolean attemptRestore() {
    List<CycleSnapshot> snapshots = repository.load();
    if (snapshots.isEmpty()) {
      AusCycleLogger.info("[RESUME] No saved state found. Using scheduler.");
      return false;
    }

    AusCycleLogger.info("[RESUME] Found saved state for " + snapshots.size()
        + " auction(s). Restoring...");

    int today = LocalDate.now(ZoneId.of("America/Sao_Paulo")).getDayOfMonth();

    for (CycleSnapshot snapshot : snapshots) {
      Optional<CycleConfig> optConfig = configManager.getCycles().stream()
          .filter(c -> c.id() == snapshot.cycleId())
          .findFirst();

      if (optConfig.isEmpty()) {
        AusCycleLogger.warn("[RESUME] Cycle " + snapshot.cycleId()
            + " not found in config. Skipping.");
        continue;
      }

      CycleConfig cycle = optConfig.get();

      if (today < cycle.startDay() || today > cycle.endDay()) {
        AusCycleLogger.info("[RESUME] Cycle " + snapshot.cycleId()
            + " is outside active date range (" + cycle.startDay() + "-" + cycle.endDay()
            + "). Today=" + today + ". Skipping.");
        repository.remove(snapshot.cycleId());
        continue;
      }

      long remainingMs = snapshot.endTimeEpoch() - System.currentTimeMillis();
      if (remainingMs <= 0) {
        AusCycleLogger.info("[RESUME] Cycle " + snapshot.cycleId()
            + " expired during downtime (was due to end at epoch "
            + snapshot.endTimeEpoch() + "). Stopping cycle.");
        AuctionHandler.stopCycle(plugin, configManager, cycle);
        repository.remove(snapshot.cycleId());
        continue;
      }

      final CycleConfig finalCycle = cycle;
      final CycleSnapshot finalSnap = snapshot;
      final long finalRemainingMs = remainingMs;

      Bukkit.getScheduler().runTask(plugin,
          () -> restoreCycle(finalCycle, finalSnap, finalRemainingMs));
    }

    return true;
  }

  private void restoreCycle(CycleConfig cycle, CycleSnapshot snapshot, long remainingMs) {
    // Start NPCs + schematic (skips already-running auctions internally)
    AuctionHandler.startCycle(plugin, configManager, cycle);

    Auction auction = AuctionManager.getAuctions().get(snapshot.auctionName());
    if (auction == null) {
      AusCycleLogger.error("[RESUME] ERROR: Auction '" + snapshot.auctionName()
          + "' not found after startCycle. Cannot restore state.");
      return;
    }

    // Apply saved state — cap to Integer.MAX_VALUE to avoid silent overflow
    long timeUnits = remainingMs / 50L;
    if (timeUnits > Integer.MAX_VALUE) {
      AusCycleLogger.warn("[RESUME] Time remainder exceeds int range ("
          + timeUnits + " ticks), capping to Integer.MAX_VALUE.");
      timeUnits = Integer.MAX_VALUE;
    }
    auction.setTime((int) timeUnits);
    auction.setBid(snapshot.bid());

    try {
      auction.setState(State.valueOf(snapshot.state()));
    } catch (IllegalArgumentException e) {
      AusCycleLogger.warn("[RESUME] Unknown state '" + snapshot.state()
          + "' — skipping setState.");
    }

    // Restore top bidder if online
    if (!snapshot.topBidderUuid().isEmpty()) {
      try {
        UUID uuid = UUID.fromString(snapshot.topBidderUuid());
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
          auction.setTopBidder(player);
          AusCycleLogger.info("[RESUME] Auction '" + snapshot.auctionName()
              + "' restored: state=" + snapshot.state()
              + ", time_remaining=" + remainingMs + "ms"
              + ", bid=" + snapshot.bid()
              + ", top_bidder=" + player.getName());
        } else {
          AusCycleLogger.warn("[RESUME] WARNING: Top bidder " + snapshot.topBidderName()
              + " (" + snapshot.topBidderUuid() + ") is offline"
              + " — bid value preserved but bidder not assigned.");
          AusCycleLogger.info("[RESUME] Auction '" + snapshot.auctionName()
              + "' restored: state=" + snapshot.state()
              + ", time_remaining=" + remainingMs + "ms"
              + ", bid=" + snapshot.bid() + " (no active bidder)");
        }
      } catch (IllegalArgumentException e) {
        AusCycleLogger.warn("[RESUME] Invalid top bidder UUID '"
            + snapshot.topBidderUuid() + "'. Skipping bidder restore.");
      }
    } else {
      AusCycleLogger.info("[RESUME] Auction '" + snapshot.auctionName()
          + "' restored: state=" + snapshot.state()
          + ", time_remaining=" + remainingMs + "ms"
          + ", bid=" + snapshot.bid());
    }

    repository.remove(snapshot.cycleId());
  }
}
