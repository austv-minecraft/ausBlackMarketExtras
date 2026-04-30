package plugin.ausBlackMarketingExtras.persistence;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;
import plugin.ausBlackMarketingExtras.model.CycleSnapshot;

import static org.junit.jupiter.api.Assertions.*;

class CycleStateRepositoryTest {

  private static Path tempDir;
  private File stateFile;
  private CycleStateRepository repository;

  @BeforeAll
  static void initLogger() throws Exception {
    tempDir = Files.createTempDirectory("austest-repo");
    AusCycleLogger.init(tempDir, Logger.getLogger("test"));
  }

  @BeforeEach
  void setUp() throws Exception {
    Path subDir = Files.createTempDirectory(tempDir, "repo-");
    stateFile = new File(subDir.toFile(), "cycle-state.yml");
    repository = new CycleStateRepository(stateFile);
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private static CycleSnapshot snapshot(int cycleId) {
    return new CycleSnapshot(
        cycleId,
        "auction-" + cycleId,
        "BIDDING",
        1746123900000L,
        5000.0,
        "123e4567-e89b-12d3-a456-426614174000",
        "PlayerName",
        0,
        1746123600000L
    );
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------

  @Test
  void saveAndLoad_roundTrip_preservesAllFields() {
    CycleSnapshot original = snapshot(1);
    repository.save(List.of(original));

    List<CycleSnapshot> loaded = repository.load();

    assertEquals(1, loaded.size());
    CycleSnapshot s = loaded.get(0);
    assertEquals(original.cycleId(), s.cycleId());
    assertEquals(original.auctionName(), s.auctionName());
    assertEquals(original.state(), s.state());
    assertEquals(original.endTimeEpoch(), s.endTimeEpoch());
    assertEquals(original.bid(), s.bid(), 0.001);
    assertEquals(original.topBidderUuid(), s.topBidderUuid());
    assertEquals(original.topBidderName(), s.topBidderName());
    assertEquals(original.bidPage(), s.bidPage());
    assertEquals(original.savedAt(), s.savedAt());
  }

  @Test
  void load_whenFileAbsent_returnsEmptyList() {
    assertFalse(stateFile.exists(), "Precondition: state file must not exist");
    List<CycleSnapshot> result = repository.load();
    assertTrue(result.isEmpty());
  }

  @Test
  void remove_existingCycleId_removesOnlyThatEntry() {
    repository.save(List.of(snapshot(1), snapshot(2), snapshot(3)));

    repository.remove(2);

    List<CycleSnapshot> remaining = repository.load();
    assertEquals(2, remaining.size());
    assertTrue(remaining.stream().noneMatch(s -> s.cycleId() == 2));
    assertTrue(remaining.stream().anyMatch(s -> s.cycleId() == 1));
    assertTrue(remaining.stream().anyMatch(s -> s.cycleId() == 3));
  }

  @Test
  void remove_nonExistentCycleId_leavesListUnchanged() {
    repository.save(List.of(snapshot(1)));

    repository.remove(99);

    assertEquals(1, repository.load().size());
  }

  @Test
  void hasState_returnsFalseWhenEmpty() {
    assertFalse(repository.hasState());
  }

  @Test
  void hasState_returnsTrueAfterSave() {
    repository.save(List.of(snapshot(1)));
    assertTrue(repository.hasState());
  }

  @Test
  void clear_deletesStateFile() {
    repository.save(List.of(snapshot(1)));
    assertTrue(stateFile.exists(), "Precondition: file must exist after save");

    repository.clear();

    assertFalse(stateFile.exists());
    assertFalse(repository.hasState());
  }

  @Test
  void saveAndLoad_multipleSnapshots_preservesOrder() {
    List<CycleSnapshot> input = List.of(snapshot(1), snapshot(2), snapshot(3));
    repository.save(input);

    List<CycleSnapshot> loaded = repository.load();

    assertEquals(3, loaded.size());
    assertEquals(1, loaded.get(0).cycleId());
    assertEquals(2, loaded.get(1).cycleId());
    assertEquals(3, loaded.get(2).cycleId());
  }

  @Test
  void saveAndLoad_withEmptyBidderFields_roundTrip() {
    CycleSnapshot noBidder = new CycleSnapshot(
        5, "auction-5", "WAITING", 0L, 0.0, "", "", 0, 1746123600000L
    );
    repository.save(List.of(noBidder));

    CycleSnapshot loaded = repository.load().get(0);

    assertEquals("", loaded.topBidderUuid());
    assertEquals("", loaded.topBidderName());
  }
}
