package plugin.ausBlackMarketingExtras.npc;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;

public final class NpcHandler {

    private NpcHandler() {}

    public static void spawn(int npcId, Location location) {
        NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
        if (npc == null) {
            AusCycleLogger.error("NPC id=" + npcId + " not found in Citizens. Skipping spawn.");
            return;
        }
        try {
            npc.spawn(location);
            AusCycleLogger.info("NPC id=" + npcId + " spawned at " + formatLoc(location) + ".");
        } catch (Exception e) {
            AusCycleLogger.error("Failed to spawn NPC id=" + npcId + ": " + e.getMessage(), e);
        }
    }

    public static void despawn(int npcId) {
        NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
        if (npc == null) {
            AusCycleLogger.error("NPC id=" + npcId + " not found in Citizens. Skipping despawn.");
            return;
        }
        try {
            npc.despawn();
            AusCycleLogger.info("NPC id=" + npcId + " despawned.");
        } catch (Exception e) {
            AusCycleLogger.error("Failed to despawn NPC id=" + npcId + ": " + e.getMessage(), e);
        }
    }

    private static String formatLoc(Location loc) {
        return loc.getWorld().getName()
            + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }
}
