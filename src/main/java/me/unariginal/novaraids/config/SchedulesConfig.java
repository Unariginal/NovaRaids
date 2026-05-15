package me.unariginal.novaraids.config;

import com.google.gson.*;
import me.unariginal.novaraids.data.schedule.*;

import java.io.*;
import java.time.ZoneId;
import java.util.List;

public class SchedulesConfig {
    public String timezone = ZoneId.systemDefault().toString();
    public List<Schedule> schedules = List.of();

//    String zoneIDRaw = "EST";
//        if (config.has("timezone"))
//    zoneIDRaw = config.get("timezone").getAsString();
//
//        if (ZoneId.getAvailableZoneIds().contains(zoneIDRaw)) {
//        zone = ZoneId.of(zoneIDRaw);
//    } else if (ZoneId.SHORT_IDS.containsKey(zoneIDRaw.toUpperCase())) {
//        String shortID = ZoneId.SHORT_IDS.get(zoneIDRaw.toUpperCase());
//        if (shortID.startsWith("+") || shortID.startsWith("-")) {
//            zone = ZoneId.ofOffset("UTC", ZoneOffset.of(ZoneId.SHORT_IDS.get(zoneIDRaw.toUpperCase())));
//        } else {
//            zone = ZoneId.of(shortID);
//        }
//    } else {
//        try {
//            zone = ZoneId.of(zoneIDRaw);
//        } catch (DateTimeException e) {
//            zone = ZoneId.systemDefault();
//            NovaRaids.LOGGER.error("[NovaRaids] Failed to parse timezone id: {}. Using system default.", zoneIDRaw);
//        }
//    }

    public Schedule getSchedule(String boss) {
        for (Schedule schedule : this.schedules) {
            for (ScheduleBoss scheduleBoss : schedule.bosses) {
                if (scheduleBoss.id.equalsIgnoreCase(boss)) {
                    return schedule;
                }
            }
        }
        return null;
    }
}
