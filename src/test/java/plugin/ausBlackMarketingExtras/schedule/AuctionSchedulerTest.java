package plugin.ausBlackMarketingExtras.schedule;

import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class AuctionSchedulerTest {

    @Test
    void calculateDelayTicks_thirtyMinutesBefore_returns36000Ticks() {
        LocalTime trigger = LocalTime.of(12, 0);
        LocalTime now = LocalTime.of(11, 30);
        assertEquals(36000L, TicksUtil.calculateDelayTicks(trigger, now));
    }

    @Test
    void calculateDelayTicks_oneSecondBefore_returns20Ticks() {
        LocalTime trigger = LocalTime.of(12, 0, 0);
        LocalTime now = LocalTime.of(11, 59, 59);
        assertEquals(20L, TicksUtil.calculateDelayTicks(trigger, now));
    }

    @Test
    void calculateDelayTicks_oneMinuteBefore_returns1200Ticks() {
        LocalTime trigger = LocalTime.of(12, 0);
        LocalTime now = LocalTime.of(11, 59);
        assertEquals(1200L, TicksUtil.calculateDelayTicks(trigger, now));
    }
}
