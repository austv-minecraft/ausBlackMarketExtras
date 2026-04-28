package plugin.ausBlackMarketingExtras.schedule;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import plugin.ausBlackMarketingExtras.AusBlackMarketingExtras;
import plugin.ausBlackMarketingExtras.auction.AuctionHandler;
import plugin.ausBlackMarketingExtras.config.ConfigManager;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;
import plugin.ausBlackMarketingExtras.model.CycleConfig;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AuctionScheduler {

    private AuctionScheduler() {}

    public static List<BukkitTask> schedule(AusBlackMarketingExtras plugin, ConfigManager config) {
        int today = LocalDate.now().getDayOfMonth();
        LocalTime now = LocalTime.now();
        LocalTime triggerTime = LocalTime.of(config.getTriggerHour(), config.getTriggerMinute());

        AusCycleLogger.info("Server started. Day: " + today + ". Checking auction cycles...");

        Optional<CycleConfig> startCycle = config.findCycleByStartDay(today);
        Optional<CycleConfig> endCycle = config.findCycleByEndDay(today);

        if (startCycle.isEmpty() && endCycle.isEmpty()) {
            AusCycleLogger.info("No auction cycle for day " + today + ".");
            return List.of();
        }

        if (!now.isBefore(triggerTime)) {
            AusCycleLogger.warn("Plugin loaded after trigger time " + triggerTime
                + " (now=" + now + "). No tasks scheduled.");
            return List.of();
        }

        long delayTicks = TicksUtil.calculateDelayTicks(triggerTime, now);
        List<BukkitTask> tasks = new ArrayList<>();

        startCycle.ifPresent(cycle -> {
            AusCycleLogger.info("Cycle " + cycle.id() + " START scheduled for "
                + triggerTime + " (in " + delayTicks + " ticks).");
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    AuctionHandler.startCycle(plugin, config, cycle);
                }
            }.runTaskLater(plugin, delayTicks);
            tasks.add(task);
        });

        endCycle.ifPresent(cycle -> {
            AusCycleLogger.info("Cycle " + cycle.id() + " STOP scheduled for "
                + triggerTime + " (in " + delayTicks + " ticks).");
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    AuctionHandler.stopCycle(plugin, config, cycle);
                }
            }.runTaskLater(plugin, delayTicks);
            tasks.add(task);
        });

        return tasks;
    }

}
