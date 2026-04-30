package plugin.ausBlackMarketingExtras.schedule;

import org.bukkit.scheduler.BukkitRunnable;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;
import plugin.ausBlackMarketingExtras.model.AuctionEntry;
import plugin.ausBlackMarketingExtras.model.CycleSnapshot;
import plugin.ausBlackMarketingExtras.persistence.AuctionStateCapture;
import plugin.ausBlackMarketingExtras.persistence.CycleStateRepository;

import java.util.ArrayList;
import java.util.List;

public final class PeriodicSaveTask extends BukkitRunnable {

  private final int cycleId;
  private final List<AuctionEntry> auctions;
  private final CycleStateRepository repository;

  public PeriodicSaveTask(int cycleId, List<AuctionEntry> auctions, CycleStateRepository repository) {
    this.cycleId = cycleId;
    this.auctions = auctions;
    this.repository = repository;
  }

  @Override
  public void run() {
    List<CycleSnapshot> snapshots = new ArrayList<>();
    for (AuctionEntry entry : auctions) {
      AuctionStateCapture.capture(cycleId, entry.name()).ifPresent(snapshots::add);
    }
    if (snapshots.isEmpty()) {
      AusCycleLogger.info("[RESUME] No active auctions in cycle " + cycleId
          + " — cancelling periodic save.");
      this.cancel();
      return;
    }
    repository.save(snapshots);
    for (CycleSnapshot snap : snapshots) {
      long remainingMs = snap.endTimeEpoch() - System.currentTimeMillis();
      AusCycleLogger.info("[RESUME] Periodic state save for cycle " + cycleId
          + ": bid=" + snap.bid() + ", time_remaining=" + remainingMs + "ms");
    }
  }
}
