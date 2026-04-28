package plugin.ausBlackMarketingExtras.npc;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;
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
            if (npc.isSpawned()) {
                npc.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
            } else {
                npc.spawn(location);
            }
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
            if (!npc.isSpawned()) {
                AusCycleLogger.warn("NPC id=" + npcId + " is not spawned according to Citizens.");
            } else {
                boolean result = npc.despawn();
                if (!result) {
                    AusCycleLogger.warn("NPC id=" + npcId + " despawn() returned false.");
                }
            }
            Entity entity = npc.getEntity();
            if (entity != null && entity.isValid()) {
                entity.remove();
                AusCycleLogger.warn("NPC id=" + npcId + " entity force-removed after despawn.");
            }
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
