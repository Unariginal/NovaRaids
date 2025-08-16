package me.unariginal.novaraids.data.schedule;

import me.unariginal.novaraids.NovaRaids;

import java.time.LocalTime;
import java.util.List;

public class SpecificSchedule extends Schedule {
    public List<LocalTime> setTimes;

    public SpecificSchedule(String type, List<ScheduleBoss> bosses, List<LocalTime> times) {
        super(type, bosses);
        this.setTimes = times;
    }

    public boolean isNextTime() {
        LocalTime now = LocalTime.now(NovaRaids.INSTANCE.schedulesConfig().zone);
        for (LocalTime time : setTimes) {
            if (time.getHour() == now.getHour() && time.getMinute() == now.getMinute() && time.getSecond() == now.getSecond()) {
                return true;
            }
        }
        return false;
    }
}
