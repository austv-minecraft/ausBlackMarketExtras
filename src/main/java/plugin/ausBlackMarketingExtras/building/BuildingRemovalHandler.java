package plugin.ausBlackMarketingExtras.building;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.World;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;
import plugin.ausBlackMarketingExtras.model.BuildingRemovalConfig;

import java.io.*;

public final class BuildingRemovalHandler {

    private BuildingRemovalHandler() {}

    public static void remove(BuildingRemovalConfig config, File backupFile) {
        World bukkitWorld = Bukkit.getWorld(config.world());
        if (bukkitWorld == null) {
            AusCycleLogger.error("BuildingRemoval: world '" + config.world() + "' not found.");
            return;
        }
        try {
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
            BlockVector3 min = BlockVector3.at(
                Math.min(config.x1(), config.x2()),
                Math.min(config.y1(), config.y2()),
                Math.min(config.z1(), config.z2())
            );
            BlockVector3 max = BlockVector3.at(
                Math.max(config.x1(), config.x2()),
                Math.max(config.y1(), config.y2()),
                Math.max(config.z1(), config.z2())
            );
            CuboidRegion region = new CuboidRegion(weWorld, min, max);

            saveRegionBackup(weWorld, region, min, backupFile);
            fillWithAir(weWorld, region);

            AusCycleLogger.info("BuildingRemoval: region removed and saved to " + backupFile.getName() + ".");
        } catch (Exception e) {
            AusCycleLogger.error("BuildingRemoval: failed to remove region.", e);
        }
    }

    public static void restore(BuildingRemovalConfig config, File backupFile) {
        if (!backupFile.exists()) {
            AusCycleLogger.error("BuildingRemoval: backup not found: " + backupFile.getPath());
            return;
        }
        World bukkitWorld = Bukkit.getWorld(config.world());
        if (bukkitWorld == null) {
            AusCycleLogger.error("BuildingRemoval: world '" + config.world() + "' not found.");
            return;
        }
        try {
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
            BlockVector3 min = BlockVector3.at(
                Math.min(config.x1(), config.x2()),
                Math.min(config.y1(), config.y2()),
                Math.min(config.z1(), config.z2())
            );

            Clipboard clipboard = loadClipboard(backupFile);
            pasteClipboard(weWorld, clipboard, min);

            boolean deleted = backupFile.delete();
            if (!deleted) AusCycleLogger.warn("BuildingRemoval: could not delete backup: " + backupFile.getPath());

            AusCycleLogger.info("BuildingRemoval: region restored from " + backupFile.getName() + ".");
        } catch (Exception e) {
            AusCycleLogger.error("BuildingRemoval: failed to restore region.", e);
        }
    }

    private static void saveRegionBackup(
        com.sk89q.worldedit.world.World weWorld,
        CuboidRegion region,
        BlockVector3 origin,
        File backupFile
    ) throws Exception {
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(origin);

        try (EditSession session = WorldEdit.getInstance().newEditSession(weWorld)) {
            ForwardExtentCopy copy = new ForwardExtentCopy(session, region, clipboard, region.getMinimumPoint());
            Operations.complete(copy);
        }

        backupFile.getParentFile().mkdirs();
        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC
                .getWriter(new FileOutputStream(backupFile))) {
            writer.write(clipboard);
        }
    }

    private static void fillWithAir(com.sk89q.worldedit.world.World weWorld, CuboidRegion region) throws Exception {
        try (EditSession session = WorldEdit.getInstance().newEditSession(weWorld)) {
            session.setBlocks(region, BlockTypes.AIR.getDefaultState().toBaseBlock());
        }
    }

    private static Clipboard loadClipboard(File file) throws IOException {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) throw new IOException("Unknown schematic format: " + file.getName());
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            return reader.read();
        }
    }

    private static void pasteClipboard(
        com.sk89q.worldedit.world.World weWorld,
        Clipboard clipboard,
        BlockVector3 target
    ) throws Exception {
        try (EditSession session = WorldEdit.getInstance().newEditSession(weWorld)) {
            Operations.complete(
                new ClipboardHolder(clipboard)
                    .createPaste(session)
                    .to(target)
                    .ignoreAirBlocks(false)
                    .build()
            );
        }
    }
}
