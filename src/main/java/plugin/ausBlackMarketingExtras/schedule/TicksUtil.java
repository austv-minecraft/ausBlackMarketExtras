package plugin.ausBlackMarketingExtras.schedule;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

final class TicksUtil {

    private TicksUtil() {}

    static long calculateDelayTicks(LocalTime triggerTime, LocalTime now) {
        long delaySeconds = now.until(triggerTime, ChronoUnit.SECONDS);
        return delaySeconds * 20L;
    }
}
