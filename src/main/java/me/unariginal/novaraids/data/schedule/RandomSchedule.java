package me.unariginal.novaraids.data.schedule;

import me.unariginal.novaraids.NovaRaids;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

public class RandomSchedule extends Schedule {
    int min_bound;
    int max_bound;
    ZonedDateTime next_random;

    public RandomSchedule(String type, List<ScheduleBoss> bosses, int min_bound, int max_bound) {
        super(type, bosses);
        this.min_bound = min_bound;
        this.max_bound = max_bound;
    }

    public void setNextRandom(ZonedDateTime date) {
        int random_seconds = new Random().nextInt(min_bound, max_bound + 1);
        next_random = date.plusSeconds(random_seconds);
    }

    public boolean isNextTime() {
        ZonedDateTime now = ZonedDateTime.now(NovaRaids.INSTANCE.schedulesConfig().zone);
        if (next_random == null) {
            setNextRandom(now);
        }
        return now.until(next_random, ChronoUnit.SECONDS) <= 0;
    }
}
