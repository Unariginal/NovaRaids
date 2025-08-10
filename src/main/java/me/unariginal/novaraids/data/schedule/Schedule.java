package me.unariginal.novaraids.data.schedule;

import java.util.List;
import java.util.Random;

public class Schedule {
    public String type;
    public List<ScheduleBoss> bosses;

    public Schedule(String type, List<ScheduleBoss> bosses) {
        this.type = type;
        this.bosses = bosses;
    }

    public ScheduleBoss getBoss() {
        double totalWeight = 0;
        for (ScheduleBoss boss : bosses) {
            totalWeight += boss.weight();
        }
        double random_weight = new Random().nextDouble(totalWeight);
        totalWeight = 0;
        for (ScheduleBoss boss : bosses) {
            totalWeight += boss.weight();
            if (random_weight < totalWeight) {
                return boss;
            }
        }

        return bosses.get(new Random().nextInt(bosses.size()));
    }
}
