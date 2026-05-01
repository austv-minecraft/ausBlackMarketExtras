package plugin.ausBlackMarketingExtras.hologram;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.DisableCause;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;
import plugin.ausBlackMarketingExtras.model.HologramConfig;

public final class HologramHandler {

    private HologramHandler() {}

    public static void show(HologramConfig config) {
        Hologram hologram = DHAPI.getHologram(config.name());
        if (hologram == null) {
            AusCycleLogger.error("Hologram '" + config.name() + "' not found in DecentHolograms. "
                + "Create it first via DecentHolograms commands.");
            return;
        }

        World world = Bukkit.getWorld(config.world());
        if (world == null) {
            AusCycleLogger.error("Hologram: world '" + config.world() + "' not found.");
            return;
        }

        Location location = new Location(world, config.x(), config.y(), config.z());
        hologram.enable();
        DHAPI.moveHologram(hologram, location);
        hologram.showAll();

        AusCycleLogger.info("Hologram '" + config.name() + "' shown at "
            + config.world() + " (" + config.x() + ", " + config.y() + ", " + config.z() + ").");
    }

    public static void hide(HologramConfig config) {
        Hologram hologram = DHAPI.getHologram(config.name());
        if (hologram == null) {
            AusCycleLogger.warn("Hologram '" + config.name() + "' not found during hide — skipping.");
            return;
        }
        hologram.disable(DisableCause.API);
        AusCycleLogger.info("Hologram '" + config.name() + "' hidden.");
    }
}
