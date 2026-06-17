package me.unariginal.novaraids.data.schedules;

import java.util.List;
import java.util.Random;

public class Schedule {
    public List<ScheduleSection> bosses;

    public ScheduleSection getBoss() {
        double totalWeight = 0;
        for (ScheduleSection boss : bosses) {
            totalWeight += boss.weight;
        }
        double randomWeight = new Random().nextDouble(totalWeight);
        totalWeight = 0;
        for (ScheduleSection boss : bosses) {
            totalWeight += boss.weight;
            if (randomWeight < totalWeight) {
                return boss;
            }
        }

        return bosses.get(new Random().nextInt(bosses.size()));
    }
}
