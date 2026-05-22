package me.unariginal.novaraids.config;

import com.google.gson.*;
import me.unariginal.novaraids.data.schedule.*;

import java.io.*;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static com.mojang.text2speech.Narrator.LOGGER;

public class SchedulesConfig {
    public String timezone = ZoneId.systemDefault().toString();
    public List<Schedule> schedules = List.of();

    public ZoneId getTimezone() {
        ZoneId zoneId;
        if (ZoneId.getAvailableZoneIds().contains(timezone)) {
            zoneId = ZoneId.of(timezone);
        } else if (ZoneId.SHORT_IDS.containsKey(timezone.toUpperCase())) {
            String shortID = ZoneId.SHORT_IDS.get(timezone.toUpperCase());
            if (shortID.startsWith("+") || shortID.startsWith("-")) {
                zoneId = ZoneId.ofOffset("UTC", ZoneOffset.of(ZoneId.SHORT_IDS.get(timezone.toUpperCase())));
            } else {
                zoneId = ZoneId.of(shortID);
            }
        } else {
            try {
                zoneId = ZoneId.of(timezone);
            } catch (DateTimeException e) {
                zoneId = ZoneId.systemDefault();
                LOGGER.error("[NovaRaids] Failed to parse timezone id: {}. Using system default.", timezone);
            }
        }

        return zoneId;
    }
}
