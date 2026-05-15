package me.unariginal.novaraids.data.schedule;

import me.unariginal.novaraids.config.ConfigManager;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;

public class RandomSchedule extends Schedule {
    public int min;
    public int max;
    public transient ZonedDateTime nextRandom;

    public RandomSchedule(String type, List<ScheduleBoss> bosses, int min, int max) {
        super(type, bosses);
        this.min = min;
        this.max = max;
    }

    public void setNextRandom(ZonedDateTime date) {
        int randomSeconds = new Random().nextInt(min, max + 1);
        nextRandom = date.plusSeconds(randomSeconds);
    }

    public boolean isNextTime() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(ConfigManager.SCHEDULES.timezone));
        if (nextRandom == null) {
            setNextRandom(now);
        }
        return now.isAfter(nextRandom);
    }
}
