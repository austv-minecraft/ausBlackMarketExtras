package plugin.ausBlackMarketingExtras.persistence;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;
import plugin.ausBlackMarketingExtras.model.CycleSnapshot;

public final class CycleStateRepository {

  private final File stateFile;
  private final Object lock = new Object();

  public CycleStateRepository(Plugin plugin) {
    this.stateFile = new File(plugin.getDataFolder(), "cycle-state.yml");
  }

  /** Package-private constructor for tests, accepting an explicit file path. */
  CycleStateRepository(File stateFile) {
    this.stateFile = stateFile;
  }

  public void save(List<CycleSnapshot> snapshots) {
    synchronized (lock) {
      YamlConfiguration config = new YamlConfiguration();
      config.set("saved-at", System.currentTimeMillis());

      List<Map<String, Object>> cyclesList = new ArrayList<>();
      for (CycleSnapshot snap : snapshots) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("cycle-id", snap.cycleId());
        entry.put("auction-name", snap.auctionName());
        entry.put("state", snap.state());
        entry.put("end-time-epoch", snap.endTimeEpoch());
        entry.put("bid", snap.bid());
        entry.put("top-bidder-uuid", snap.topBidderUuid());
        entry.put("top-bidder-name", snap.topBidderName());
        entry.put("bid-page", snap.bidPage());
        entry.put("saved-at", snap.savedAt());
        cyclesList.add(entry);
      }
      config.set("cycles", cyclesList);

      try {
        File parent = stateFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
          AusCycleLogger.error("Failed to create directory: " + parent.getAbsolutePath());
          return;
        }
        config.save(stateFile);
      } catch (IOException e) {
        AusCycleLogger.error("Failed to save cycle-state.yml", e);
      }
    }
  }

  public List<CycleSnapshot> load() {
    synchronized (lock) {
      if (!stateFile.exists()) {
        return Collections.emptyList();
      }
      try {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(stateFile);
        List<Map<?, ?>> rawList = config.getMapList("cycles");
        if (rawList == null || rawList.isEmpty()) {
          return Collections.emptyList();
        }
        List<CycleSnapshot> result = new ArrayList<>();
        for (Map<?, ?> raw : rawList) {
          CycleSnapshot snap = parseSnapshot(raw);
          if (snap != null) {
            result.add(snap);
          }
        }
        return Collections.unmodifiableList(result);
      } catch (Exception e) {
        AusCycleLogger.error("Corrupted cycle-state.yml — falling back to scheduler. "
            + "Rename or delete the file to suppress this warning. Error: " + e.getMessage(), e);
        return Collections.emptyList();
      }
    }
  }

  private CycleSnapshot parseSnapshot(Map<?, ?> raw) {
    try {
      Object cycleIdObj = raw.get("cycle-id");
      Object auctionNameObj = raw.get("auction-name");
      Object stateObj = raw.get("state");
      Object endTimeObj = raw.get("end-time-epoch");
      Object bidObj = raw.get("bid");
      Object bidPageObj = raw.get("bid-page");
      Object savedAtObj = raw.get("saved-at");

      if (!(cycleIdObj instanceof Number)
          || !(auctionNameObj instanceof String)
          || !(stateObj instanceof String)
          || !(endTimeObj instanceof Number)
          || !(bidObj instanceof Number)
          || !(bidPageObj instanceof Number)
          || !(savedAtObj instanceof Number)) {
        AusCycleLogger.warn("[RESUME] Skipping malformed snapshot entry: " + raw);
        return null;
      }

      String topBidderUuid = raw.get("top-bidder-uuid") instanceof String s ? s : "";
      String topBidderName = raw.get("top-bidder-name") instanceof String s ? s : "";

      return new CycleSnapshot(
          ((Number) cycleIdObj).intValue(),
          (String) auctionNameObj,
          (String) stateObj,
          ((Number) endTimeObj).longValue(),
          ((Number) bidObj).doubleValue(),
          topBidderUuid,
          topBidderName,
          ((Number) bidPageObj).intValue(),
          ((Number) savedAtObj).longValue()
      );
    } catch (Exception e) {
      AusCycleLogger.warn("[RESUME] Failed to parse snapshot entry: " + e.getMessage());
      return null;
    }
  }

  public void remove(int cycleId) {
    synchronized (lock) {
      List<CycleSnapshot> current = new ArrayList<>(load());
      current.removeIf(snap -> snap.cycleId() == cycleId);
      save(current);
    }
  }

  public void clear() {
    synchronized (lock) {
      if (stateFile.exists() && !stateFile.delete()) {
        AusCycleLogger.warn("[RESUME] Failed to delete cycle-state.yml.");
      }
    }
  }

  public boolean hasState() {
    return stateFile.exists() && !load().isEmpty();
  }
}
