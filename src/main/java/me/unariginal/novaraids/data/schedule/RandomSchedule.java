package me.unariginal.novaraids.data.schedule;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;

public class RandomSchedule extends Schedule {
    int min_bound;
    int max_bound;

    public RandomSchedule(String type, List<ScheduleBoss> bosses, int min_bound, int max_bound) {
        super(type, bosses);
        this.min_bound = min_bound;
        this.max_bound = max_bound;
    }

    public ZonedDateTime nextRandom(ZonedDateTime date) {
        int random_seconds = new Random().nextInt(min_bound, max_bound + 1);
        return date.plusSeconds(random_seconds);
    }
}
