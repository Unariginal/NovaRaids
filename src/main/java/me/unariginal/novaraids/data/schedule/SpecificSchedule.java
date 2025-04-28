package me.unariginal.novaraids.data.schedule;

import me.unariginal.novaraids.NovaRaids;

import java.time.LocalTime;
import java.util.List;

public class SpecificSchedule extends Schedule {
    List<LocalTime> set_times;

    public SpecificSchedule(String type, List<ScheduleBoss> bosses, List<LocalTime> times) {
        super(type, bosses);
        this.set_times = times;
    }

    public boolean isNextTime() {
        LocalTime now = LocalTime.now(NovaRaids.INSTANCE.schedulesConfig().zone);
        for (LocalTime time : set_times) {
            if (time.getHour() == now.getHour() && time.getMinute() == now.getMinute() && time.getSecond() == now.getSecond()) {
                return true;
            }
        }
        return false;
    }
}
