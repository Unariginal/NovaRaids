package me.unariginal.novaraids.data.schedules;

import java.time.ZonedDateTime;
import java.util.Random;

import static me.unariginal.novaraids.config.ConfigManager.SCHEDULES;

public class RandomSchedule extends Schedule {
    public int minSeconds;
    public int maxSeconds;
    public transient ZonedDateTime nextRandom;

    public void setNextRandom(ZonedDateTime date) {
        int randomSeconds = new Random().nextInt(minSeconds, maxSeconds + 1);
        nextRandom = date.plusSeconds(randomSeconds);
    }

    public boolean isNextTime() {
        ZonedDateTime now = ZonedDateTime.now(SCHEDULES.getTimezone());
        if (nextRandom == null) {
            setNextRandom(now);
        }
        return now.isAfter(nextRandom);
    }
}
