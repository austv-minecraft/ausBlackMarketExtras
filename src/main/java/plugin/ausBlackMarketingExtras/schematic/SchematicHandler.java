package plugin.ausBlackMarketingExtras.schematic;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Location;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;

import java.io.*;

public final class SchematicHandler {

    private SchematicHandler() {}

    public static void paste(Location pasteLocation, File schematicFile, File backupFile) {
        try {
            Clipboard schematic = loadClipboard(schematicFile);
            saveBackup(pasteLocation, schematic, backupFile);
            pasteClipboard(schematic, pasteLocation);
            AusCycleLogger.info("Schematic pasted at " + formatLoc(pasteLocation) + ".");
        } catch (Exception e) {
            AusCycleLogger.error("Failed to paste schematic at " + formatLoc(pasteLocation), e);
        }
    }

    public static void restore(Location pasteLocation, File backupFile) {
        if (!backupFile.exists()) {
            AusCycleLogger.error("Backup not found: " + backupFile.getPath() + ". Cannot restore.");
            return;
        }
        try {
            Clipboard backup = loadClipboard(backupFile);
            pasteClipboard(backup, pasteLocation);
            boolean deleted = backupFile.delete();
            if (!deleted) AusCycleLogger.warn("Could not delete backup: " + backupFile.getPath());
            AusCycleLogger.info("Area restored at " + formatLoc(pasteLocation) + ".");
        } catch (Exception e) {
            AusCycleLogger.error("Failed to restore area at " + formatLoc(pasteLocation), e);
        }
    }

    private static Clipboard loadClipboard(File file) throws IOException {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) throw new IOException("Unknown schematic format: " + file.getName());
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            return reader.read();
        }
    }

    private static void saveBackup(Location origin, Clipboard reference, File backupFile) throws Exception {
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(origin.getWorld());
        BlockVector3 originVec = BlockVector3.at(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
        BlockVector3 clipOrigin = reference.getOrigin();

        BlockVector3 min = originVec.add(reference.getMinimumPoint().subtract(clipOrigin));
        BlockVector3 max = originVec.add(reference.getMaximumPoint().subtract(clipOrigin));

        CuboidRegion region = new CuboidRegion(weWorld, min, max);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
            Operations.complete(copy);
        }

        backupFile.getParentFile().mkdirs();
        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC
                .getWriter(new FileOutputStream(backupFile))) {
            writer.write(clipboard);
        }
    }

    private static void pasteClipboard(Clipboard clipboard, Location location) throws Exception {
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(location.getWorld());
        BlockVector3 target = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            Operation operation = new ClipboardHolder(clipboard)
                .createPaste(editSession)
                .to(target)
                .ignoreAirBlocks(false)
                .build();
            Operations.complete(operation);
        }
    }

    private static String formatLoc(Location loc) {
        return loc.getWorld().getName()
            + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }
}
