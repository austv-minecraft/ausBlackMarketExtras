package plugin.ausBlackMarketingExtras.auction;

import com.artillexstudios.axdarkauctions.auctions.Auction;
import com.artillexstudios.axdarkauctions.auctions.AuctionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import plugin.ausBlackMarketingExtras.AusBlackMarketingExtras;
import plugin.ausBlackMarketingExtras.config.ConfigManager;
import plugin.ausBlackMarketingExtras.discord.DiscordWebhook;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;
import plugin.ausBlackMarketingExtras.model.AuctionEntry;
import plugin.ausBlackMarketingExtras.model.CycleConfig;
import plugin.ausBlackMarketingExtras.building.BuildingRemovalHandler;
import plugin.ausBlackMarketingExtras.hologram.HologramHandler;
import plugin.ausBlackMarketingExtras.npc.NpcHandler;
import plugin.ausBlackMarketingExtras.schematic.SchematicHandler;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import plugin.ausBlackMarketingExtras.model.CycleSnapshot;
import plugin.ausBlackMarketingExtras.persistence.AuctionStateCapture;
import plugin.ausBlackMarketingExtras.persistence.CycleStateRepository;
import plugin.ausBlackMarketingExtras.schedule.PeriodicSaveTask;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.scheduler.BukkitTask;

public final class AuctionHandler {

    private static final Map<Integer, BukkitTask> periodicSaveTasks = new HashMap<>();
    private static CycleStateRepository stateRepository;

    private AuctionHandler() {}

    public static void init(CycleStateRepository repo) {
        stateRepository = repo;
    }

    public static void startCycle(AusBlackMarketingExtras plugin, ConfigManager config, CycleConfig cycle) {
        startCycle(plugin, config, cycle, false);
    }

    public static void startCycleForRestore(AusBlackMarketingExtras plugin, ConfigManager config, CycleConfig cycle) {
        startCycle(plugin, config, cycle, true);
    }

    private static void startCycle(
        AusBlackMarketingExtras plugin,
        ConfigManager config,
        CycleConfig cycle,
        boolean forceRestartRunningAuctions
    ) {
        AusCycleLogger.info("=== START cycle " + cycle.id()
            + " (day " + cycle.startDay() + " → " + cycle.endDay() + ") ===");

        World world = Bukkit.getWorld(cycle.world());
        if (world == null) {
            AusCycleLogger.error("World '" + cycle.world() + "' not found. Aborting cycle " + cycle.id() + ".");
            return;
        }

        Location cycleLoc = new Location(world, cycle.x(), cycle.y(), cycle.z());
        Location schematicLoc = cycleLoc.clone().add(0, -1, 0);

        if (cycle.buildingRemoval() != null) {
            File removalBackup = new File(plugin.getDataFolder(),
                "schematics/backups/building_removal_" + cycle.id() + ".schem");
            BuildingRemovalHandler.remove(cycle.buildingRemoval(), removalBackup);
        }

        File schematicFile = new File(plugin.getDataFolder(), "schematics/" + config.getSchematic());
        File backupFile = new File(plugin.getDataFolder(), "schematics/backups/backup_" + cycle.id() + ".schem");
        SchematicHandler.paste(schematicLoc, schematicFile, backupFile);

        if (cycle.hologram() != null) {
            HologramHandler.show(cycle.hologram());
        }

        for (AuctionEntry entry : cycle.auctions()) {
            Auction auction = AuctionManager.getAuctions().get(entry.name());
            if (auction == null) {
                AusCycleLogger.error("Auction '" + entry.name() + "' not found in AuctionManager. Skipping.");
                continue;
            }
            if (auction.isRunning()) {
                if (!forceRestartRunningAuctions) {
                    AusCycleLogger.warn("Auction '" + entry.name() + "' is already running. Skipping.");
                    continue;
                }
                AusCycleLogger.warn("Auction '" + entry.name() + "' is already running during restore. Forcing restart.");
                auction.stop();
            }
            NpcHandler.spawn(entry.npcId(), cycleLoc);
            auction.start();
            AusCycleLogger.info("Auction '" + entry.name() + "' started.");
        }

        if (config.isDiscordEnabled() && !config.getWebhookUrl().isBlank()) {
            Map<String, String> ph = Map.of(
                "start_day", String.valueOf(cycle.startDay()),
                "end_day", String.valueOf(cycle.endDay()),
                "cycle", String.valueOf(cycle.id()),
                "auction_count", String.valueOf(cycle.auctions().size())
            );
            DiscordWebhook.send(config.getWebhookUrl(), config.getStartEmbed().withPlaceholders(ph));
        }

        if (config.isBroadcastEnabled()) {
            broadcastChat(config.getBroadcastStartMessage());
            broadcastTitleAndSound(config, config.getBroadcastStartTitle(), config.getBroadcastStartSubtitle());
        }

        // Persist initial state and start periodic save
        if (stateRepository != null) {
            List<CycleSnapshot> initialSnapshots = new ArrayList<>();
            for (AuctionEntry entry : cycle.auctions()) {
                AuctionStateCapture.capture(cycle.id(), entry.name()).ifPresent(initialSnapshots::add);
            }
            if (!initialSnapshots.isEmpty()) {
                stateRepository.save(initialSnapshots);
            }

            BukkitTask existing = periodicSaveTasks.remove(cycle.id());
            if (existing != null) {
                existing.cancel();
            }
            BukkitTask saveTask = new PeriodicSaveTask(cycle.id(), cycle.auctions(), stateRepository)
                .runTaskTimerAsynchronously(plugin, 600L, 600L);
            periodicSaveTasks.put(cycle.id(), saveTask);
        }

        AusCycleLogger.info("=== Cycle " + cycle.id() + " start complete. ===");
    }

    /**
     * Captures and persists the current state of all active cycles immediately.
     * Used on disable/reload so that state is not lost between JVM shutdowns.
     *
     * @param config config manager used to look up cycle auction lists
     */
    public static void saveAllActiveCycles(ConfigManager config) {
        if (stateRepository == null) {
            return;
        }
        List<CycleSnapshot> snapshots = new ArrayList<>();
        for (CycleConfig cycle : config.getCycles()) {
            for (AuctionEntry entry : cycle.auctions()) {
                AuctionStateCapture.capture(cycle.id(), entry.name()).ifPresent(snapshots::add);
            }
        }
        if (!snapshots.isEmpty()) {
            stateRepository.save(snapshots);
            AusCycleLogger.info("[RESUME] saveAllActiveCycles: persisted " + snapshots.size()
                + " snapshot(s) before shutdown/reload.");
        }
    }

    public static void stopCycle(AusBlackMarketingExtras plugin, ConfigManager config, CycleConfig cycle) {
        AusCycleLogger.info("=== STOP cycle " + cycle.id() + " (day " + cycle.endDay() + ") ===");

        // Cancel periodic save task for this cycle
        BukkitTask saveTask = periodicSaveTasks.remove(cycle.id());
        if (saveTask != null) {
            saveTask.cancel();
        }

        World world = Bukkit.getWorld(cycle.world());
        if (world == null) {
            AusCycleLogger.error("World '" + cycle.world() + "' not found. Aborting stop of cycle " + cycle.id() + ".");
            return;
        }

        Location cycleLoc = new Location(world, cycle.x(), cycle.y(), cycle.z());
        Location schematicLoc = cycleLoc.clone().add(0, -1, 0);

        for (AuctionEntry entry : cycle.auctions()) {
            Auction auction = AuctionManager.getAuctions().get(entry.name());
            if (auction == null) {
                AusCycleLogger.error("Auction '" + entry.name() + "' not found in AuctionManager. Skipping.");
                continue;
            }
            auction.stop();
            NpcHandler.despawn(entry.npcId());
            AusCycleLogger.info("Auction '" + entry.name() + "' stopped.");
        }

        if (cycle.hologram() != null) {
            HologramHandler.hide(cycle.hologram());
        }

        // Remove persisted state for this cycle since it ended cleanly
        if (stateRepository != null) {
            stateRepository.remove(cycle.id());
        }

        File backupFile = new File(plugin.getDataFolder(), "schematics/backups/backup_" + cycle.id() + ".schem");
        SchematicHandler.restore(schematicLoc, backupFile);

        if (cycle.buildingRemoval() != null) {
            File removalBackup = new File(plugin.getDataFolder(),
                "schematics/backups/building_removal_" + cycle.id() + ".schem");
            BuildingRemovalHandler.restore(cycle.buildingRemoval(), removalBackup);
        }

        if (config.isDiscordEnabled() && !config.getWebhookUrl().isBlank()) {
            Map<String, String> ph = Map.of(
                "start_day", String.valueOf(cycle.startDay()),
                "end_day", String.valueOf(cycle.endDay()),
                "cycle", String.valueOf(cycle.id()),
                "auction_count", String.valueOf(cycle.auctions().size())
            );
            DiscordWebhook.send(config.getWebhookUrl(), config.getEndEmbed().withPlaceholders(ph));
        }

        if (config.isBroadcastEnabled()) {
            broadcastChat(config.getBroadcastEndMessage());
            broadcastTitleAndSound(config, config.getBroadcastEndTitle(), config.getBroadcastEndSubtitle());
        }

        AusCycleLogger.info("=== Cycle " + cycle.id() + " stop complete. ===");
    }

    private static void broadcastChat(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) return;
        String converted = rawMessage.replaceAll("(?i)&#([0-9A-Fa-f]{6})", "<#$1>");
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(converted));
    }

    private static void broadcastTitleAndSound(ConfigManager config, String rawTitle, String rawSubtitle) {
        if (rawTitle == null || rawTitle.isBlank()) return;

        String titleStr = rawTitle.replaceAll("(?i)&#([0-9A-Fa-f]{6})", "<#$1>");
        String subtitleStr = (rawSubtitle != null && !rawSubtitle.isBlank())
            ? rawSubtitle.replaceAll("(?i)&#([0-9A-Fa-f]{6})", "<#$1>")
            : null;

        Component titleComp = MiniMessage.miniMessage().deserialize(titleStr);
        Component subtitleComp = subtitleStr != null
            ? MiniMessage.miniMessage().deserialize(subtitleStr)
            : Component.empty();

        Title.Times times = Title.Times.times(
            Duration.ofMillis(config.getTitleFadeIn() * 50L),
            Duration.ofMillis(config.getTitleStay() * 50L),
            Duration.ofMillis(config.getTitleFadeOut() * 50L)
        );
        Title title = Title.title(titleComp, subtitleComp, times);

        Sound sound = null;
        String soundName = config.getBroadcastSound();
        if (soundName != null && !soundName.isBlank()) {
            try {
                sound = Sound.valueOf(soundName.toUpperCase());
            } catch (IllegalArgumentException e) {
                AusCycleLogger.warn("[ausBlackMarketing] Sound inválido: " + soundName);
            }
        }

        Sound finalSound = sound;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(title);
            if (finalSound != null) {
                player.playSound(player.getLocation(), finalSound, config.getSoundVolume(), config.getSoundPitch());
            }
        }
    }
}
