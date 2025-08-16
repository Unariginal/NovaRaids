package me.unariginal.novaraids.data.schedule;

import me.unariginal.novaraids.NovaRaids;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;

public class RandomSchedule extends Schedule {
    public int minBound;
    public int maxBound;
    public ZonedDateTime nextRandom;

    public RandomSchedule(String type, List<ScheduleBoss> bosses, int minBound, int maxBound) {
        super(type, bosses);
        this.minBound = minBound;
        this.maxBound = maxBound;
    }

    public void setNextRandom(ZonedDateTime date) {
        int randomSeconds = new Random().nextInt(minBound, maxBound + 1);
        nextRandom = date.plusSeconds(randomSeconds);
    }

    public boolean isNextTime() {
        ZonedDateTime now = ZonedDateTime.now(NovaRaids.INSTANCE.schedulesConfig().zone);
        if (nextRandom == null) {
            setNextRandom(now);
        }
        return now.isAfter(nextRandom);
    }
}
