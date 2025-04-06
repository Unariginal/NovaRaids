package me.unariginal.novaraids.data.schedule;

import java.util.List;
import java.util.Random;

public class Schedule {
    String type;
    List<ScheduleBoss> bosses;

    public Schedule(String type, List<ScheduleBoss> bosses) {
        this.type = type;
        this.bosses = bosses;
    }

    public ScheduleBoss getBoss() {
        double total_weight = 0;
        for (ScheduleBoss boss : bosses) {
            total_weight += boss.weight();
        }
        double random_weight = new Random().nextDouble(total_weight);
        total_weight = 0;
        for (ScheduleBoss boss : bosses) {
            total_weight += boss.weight();
            if (random_weight < total_weight) {
                return boss;
            }
        }

        return bosses.get(new Random().nextInt(bosses.size()));
    }
}
