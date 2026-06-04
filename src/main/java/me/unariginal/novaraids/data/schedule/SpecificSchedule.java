package me.unariginal.novaraids.data.schedule;

import me.unariginal.novaraids.NovaRaids;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static me.unariginal.novaraids.config.ConfigManager.SCHEDULES;

public class SpecificSchedule extends Schedule {
    public List<String> times;
    public transient List<LocalTime> localTimes = new ArrayList<>();

    public boolean isNextTime() {
        LocalTime now = LocalTime.now(SCHEDULES.getTimezone());

        if (localTimes != null) {
            for (LocalTime time : localTimes) {
                if (time.getHour() == now.getHour() && time.getMinute() == now.getMinute() && time.getSecond() == now.getSecond()) {
                    return true;
                }
            }
        } else {
            NovaRaids.LOGGER.info("[NovaRaids] localTimes was null! Attempting to fill");
            localTimes = new ArrayList<>();
            this.times.forEach(time -> localTimes.add(LocalTime.parse(time)));
        }
        return false;
    }

    public void fillLocalTimes() {
        localTimes = new ArrayList<>();
        this.times.forEach(time -> localTimes.add(LocalTime.parse(time)));
    }
}
