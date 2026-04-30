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

  public CycleStateRepository(Plugin plugin) {
    this.stateFile = new File(plugin.getDataFolder(), "cycle-state.yml");
  }

  /** Package-private constructor for tests, accepting an explicit file path. */
  CycleStateRepository(File stateFile) {
    this.stateFile = stateFile;
  }

  public void save(List<CycleSnapshot> snapshots) {
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
      if (!stateFile.getParentFile().exists()) {
        stateFile.getParentFile().mkdirs();
      }
      config.save(stateFile);
    } catch (IOException e) {
      AusCycleLogger.error("Failed to save cycle-state.yml", e);
    }
  }

  public List<CycleSnapshot> load() {
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
        result.add(new CycleSnapshot(
            ((Number) raw.get("cycle-id")).intValue(),
            (String) raw.get("auction-name"),
            (String) raw.get("state"),
            ((Number) raw.get("end-time-epoch")).longValue(),
            ((Number) raw.get("bid")).doubleValue(),
            (String) raw.get("top-bidder-uuid"),
            (String) raw.get("top-bidder-name"),
            ((Number) raw.get("bid-page")).intValue(),
            ((Number) raw.get("saved-at")).longValue()
        ));
      }
      return Collections.unmodifiableList(result);
    } catch (Exception e) {
      AusCycleLogger.warn("Failed to load cycle-state.yml: " + e.getMessage());
      return Collections.emptyList();
    }
  }

  public void remove(int cycleId) {
    List<CycleSnapshot> current = new ArrayList<>(load());
    current.removeIf(snap -> snap.cycleId() == cycleId);
    save(current);
  }

  public void clear() {
    stateFile.delete();
  }

  public boolean hasState() {
    return stateFile.exists() && !load().isEmpty();
  }
}
