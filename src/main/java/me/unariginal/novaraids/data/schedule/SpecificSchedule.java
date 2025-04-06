package me.unariginal.novaraids.data.schedule;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public class SpecificSchedule extends Schedule {
    List<LocalTime> set_times;

    public SpecificSchedule(String type, List<ScheduleBoss> bosses, List<LocalTime> times) {
        super(type, bosses);
        this.set_times = times;
    }

    public boolean isNextTime() {
        // TODO: Change Zone ID To Schedules.java zone id.
        LocalTime now = LocalTime.now(ZoneId.systemDefault());
        for (LocalTime time : set_times) {
            if (time.getHour() == now.getHour() && time.getMinute() == now.getMinute() && time.getSecond() == now.getSecond()) {
                return true;
            }
        }
        return false;
    }
}
