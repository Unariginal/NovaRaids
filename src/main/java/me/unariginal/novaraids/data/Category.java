package me.unariginal.novaraids.data;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public record Category(
        String name,
        boolean require_voucher,
        int min_players,
        int max_players,
        int min_wait_time,
        int max_wait_time,
        List<LocalTime> set_times,
        Map<List<String>, List<String>> rewards
) {}
