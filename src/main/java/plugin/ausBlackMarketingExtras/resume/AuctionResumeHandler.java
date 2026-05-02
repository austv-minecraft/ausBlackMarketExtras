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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class AuctionResumeHandler {

  private static final int RESTORE_RETRY_LIMIT = 10;
  private static final long RESTORE_RETRY_DELAY_TICKS = 20L;

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

    Map<Integer, List<CycleSnapshot>> snapshotsByCycle = new HashMap<>();
    for (CycleSnapshot snapshot : snapshots) {
      snapshotsByCycle.computeIfAbsent(snapshot.cycleId(), ignored -> new ArrayList<>()).add(snapshot);
    }

    for (Map.Entry<Integer, List<CycleSnapshot>> cycleEntry : snapshotsByCycle.entrySet()) {
      int cycleId = cycleEntry.getKey();
      List<CycleSnapshot> cycleSnapshots = cycleEntry.getValue();
      Optional<CycleConfig> optConfig = configManager.getCycles().stream()
          .filter(c -> c.id() == cycleId)
          .findFirst();

      if (optConfig.isEmpty()) {
        AusCycleLogger.warn("[RESUME] Cycle " + cycleId
            + " not found in config. Skipping.");
        continue;
      }

      CycleConfig cycle = optConfig.get();

      // Skip date-range check if state is fresh (saved within the last 30 minutes).
      // Fresh state means this restore was triggered by /axda reload (state saved seconds
      // ago), so the day-of-month check would wrongly reject it. Stale state (server was
      // down for hours/days) still goes through the date-range filter.
      long maxSavedAt = cycleSnapshots.stream()
          .mapToLong(CycleSnapshot::savedAt)
          .max()
          .orElse(0L);
      boolean isFreshState = (System.currentTimeMillis() - maxSavedAt) < 30 * 60 * 1000L;

      if (!isFreshState && (today < cycle.startDay() || today > cycle.endDay())) {
        AusCycleLogger.info("[RESUME] Cycle " + cycleId
            + " is outside active date range (" + cycle.startDay() + "-" + cycle.endDay()
            + "). Today=" + today + ". Skipping.");
        repository.remove(cycleId);
        continue;
      }

      long maxEndEpoch = cycleSnapshots.stream()
          .mapToLong(CycleSnapshot::endTimeEpoch)
          .max()
          .orElse(0L);

      if (maxEndEpoch <= System.currentTimeMillis()) {
        AusCycleLogger.info("[RESUME] Cycle " + cycleId
            + " expired during downtime (was due to end at epoch " + maxEndEpoch + "). Stopping cycle.");
        AuctionHandler.stopCycle(plugin, configManager, cycle);
        repository.remove(cycleId);
        continue;
      }

      final CycleConfig finalCycle = cycle;
      final List<CycleSnapshot> finalSnapshots = List.copyOf(cycleSnapshots);

      Bukkit.getScheduler().runTask(plugin,
          () -> restoreCycleWithRetry(finalCycle, finalSnapshots, 0));
    }

    return true;
  }

  private void restoreCycleWithRetry(CycleConfig cycle, List<CycleSnapshot> snapshots, int attempt) {
    List<String> missingAuctions = new ArrayList<>();
    for (CycleSnapshot snapshot : snapshots) {
      if (AuctionManager.getAuctions().get(snapshot.auctionName()) == null) {
        missingAuctions.add(snapshot.auctionName());
      }
    }

    if (!missingAuctions.isEmpty()) {
      if (attempt >= RESTORE_RETRY_LIMIT) {
        AusCycleLogger.error("[RESUME] Failed to restore cycle " + cycle.id()
            + " after " + RESTORE_RETRY_LIMIT + " attempts. Missing auctions: " + missingAuctions
            + ". Keeping saved state for next startup.");
        return;
      }
      int nextAttempt = attempt + 1;
      AusCycleLogger.warn("[RESUME] Cycle " + cycle.id() + " restore postponed (attempt "
          + nextAttempt + "/" + RESTORE_RETRY_LIMIT + "). Missing auctions: " + missingAuctions
          + ". Retrying in " + RESTORE_RETRY_DELAY_TICKS + " ticks.");
      Bukkit.getScheduler().runTaskLater(
          plugin,
          () -> restoreCycleWithRetry(cycle, snapshots, nextAttempt),
          RESTORE_RETRY_DELAY_TICKS
      );
      return;
    }

    // Detect reload vs restart: if any auction is already running, the auctions survived
    // the /axda reload — stopping and restarting them would clear the display block item
    // and current bid. In that case restore only infrastructure (schematic, hologram, NPCs).
    // For server restart, no auctions are running, so full startCycleForRestore is needed.
    boolean anyRunning = snapshots.stream().anyMatch(s -> {
      Auction a = AuctionManager.getAuctions().get(s.auctionName());
      return a != null && a.isRunning();
    });

    if (anyRunning) {
      AusCycleLogger.info("[RESUME] Cycle " + cycle.id()
          + ": auctions already running (post-reload) — restoring infrastructure only.");
      AuctionHandler.restoreInfrastructureOnly(plugin, configManager, cycle);
    } else {
      AuctionHandler.startCycleForRestore(plugin, configManager, cycle);
    }
    boolean hadRestoreFailure = false;

    for (CycleSnapshot snapshot : snapshots) {
      long remainingMs = snapshot.endTimeEpoch() - System.currentTimeMillis();
      if (remainingMs <= 0) {
        continue;
      }

      Auction auction = AuctionManager.getAuctions().get(snapshot.auctionName());
      if (auction == null) {
        AusCycleLogger.error("[RESUME] ERROR: Auction '" + snapshot.auctionName()
            + "' not found after startCycle. Cannot restore state.");
        hadRestoreFailure = true;
        continue;
      }

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
    }

    if (!hadRestoreFailure) {
      repository.remove(cycle.id());
    } else {
      AusCycleLogger.warn("[RESUME] Cycle " + cycle.id()
          + " had restore errors. Keeping saved state for retry on next startup.");
    }
  }
}
