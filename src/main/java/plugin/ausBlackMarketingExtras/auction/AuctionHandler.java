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
import plugin.ausBlackMarketingExtras.npc.NpcHandler;
import plugin.ausBlackMarketingExtras.schematic.SchematicHandler;

import java.io.File;
import java.util.Map;

public final class AuctionHandler {

    private AuctionHandler() {}

    public static void startCycle(AusBlackMarketingExtras plugin, ConfigManager config, CycleConfig cycle) {
        AusCycleLogger.info("=== START cycle " + cycle.id()
            + " (day " + cycle.startDay() + " → " + cycle.endDay() + ") ===");

        World world = Bukkit.getWorld(cycle.world());
        if (world == null) {
            AusCycleLogger.error("World '" + cycle.world() + "' not found. Aborting cycle " + cycle.id() + ".");
            return;
        }

        Location cycleLoc = new Location(world, cycle.x(), cycle.y(), cycle.z());
        Location schematicLoc = cycleLoc.clone().add(0, -1, 0);

        File schematicFile = new File(plugin.getDataFolder(), "schematics/" + config.getSchematic());
        File backupFile = new File(plugin.getDataFolder(), "schematics/backups/backup_" + cycle.id() + ".schem");
        SchematicHandler.paste(schematicLoc, schematicFile, backupFile);

        for (AuctionEntry entry : cycle.auctions()) {
            Auction auction = AuctionManager.getAuctions().get(entry.name());
            if (auction == null) {
                AusCycleLogger.error("Auction '" + entry.name() + "' not found in AuctionManager. Skipping.");
                continue;
            }
            if (auction.isRunning()) {
                AusCycleLogger.warn("Auction '" + entry.name() + "' is already running. Skipping.");
                continue;
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

        AusCycleLogger.info("=== Cycle " + cycle.id() + " start complete. ===");
    }

    public static void stopCycle(AusBlackMarketingExtras plugin, ConfigManager config, CycleConfig cycle) {
        AusCycleLogger.info("=== STOP cycle " + cycle.id() + " (day " + cycle.endDay() + ") ===");

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

        File backupFile = new File(plugin.getDataFolder(), "schematics/backups/backup_" + cycle.id() + ".schem");
        SchematicHandler.restore(schematicLoc, backupFile);

        if (config.isDiscordEnabled() && !config.getWebhookUrl().isBlank()) {
            Map<String, String> ph = Map.of(
                "start_day", String.valueOf(cycle.startDay()),
                "end_day", String.valueOf(cycle.endDay()),
                "cycle", String.valueOf(cycle.id()),
                "auction_count", String.valueOf(cycle.auctions().size())
            );
            DiscordWebhook.send(config.getWebhookUrl(), config.getEndEmbed().withPlaceholders(ph));
        }

        AusCycleLogger.info("=== Cycle " + cycle.id() + " stop complete. ===");
    }
}
