package me.unariginal.novaraids.data;

import me.unariginal.novaraids.data.rewards.DistributionSection;

import java.time.LocalTime;
import java.util.List;

public record Category(
        String name,
        boolean require_pass,
        int min_players,
        int max_players,
        int min_wait_time,
        int max_wait_time,
        List<LocalTime> set_times,
        List<DistributionSection> rewards
) {}
