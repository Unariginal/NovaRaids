package me.unariginal.novaraids.data;

import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.rewards.DistributionSection;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

public class Category {
    private LocalDateTime next_time = null;
    private final String name;
    private final boolean require_pass;
    private final int min_players;
    private final int max_players;
    private final int min_wait_time;
    private final int max_wait_time;
    private final List<LocalTime> set_times;
    private final List<DistributionSection> rewards;

    public Category(String name,
                    boolean require_pass,
                    int min_players,
                    int max_players,
                    int min_wait_time,
                    int max_wait_time,
                    List<LocalTime> set_times,
                    List<DistributionSection> rewards) {
        this.name = name;
        this.require_pass = require_pass;
        this.min_players = min_players;
        this.max_players = max_players;
        this.min_wait_time = min_wait_time;
        this.max_wait_time = max_wait_time;
        this.set_times = set_times;
        this.rewards = rewards;
    }

    public void new_next_time(LocalDateTime now) {
        if (min_wait_time > 0 && max_wait_time >= min_wait_time) {
            if (next_time == null || now.isAfter(next_time)) {
                int random_seconds = new Random().nextInt(min_wait_time, max_wait_time + 1);
                next_time = now.plusSeconds(random_seconds);
                NovaRaids.INSTANCE.logInfo("[RAIDS] New next time: " + next_time);
                return;
            }
        }
        next_time = null;
    }

    public LocalDateTime next_time() {
        return next_time;
    }

    public String name() {
        return name;
    }

    public boolean require_pass() {
        return require_pass;
    }

    public int min_players() {
        return min_players;
    }

    public int max_players() {
        return max_players;
    }

    public int min_wait_time() {
        return min_wait_time;
    }

    public int max_wait_time() {
        return max_wait_time;
    }

    public List<LocalTime> set_times() {
        return set_times;
    }

    public List<DistributionSection> rewards() {
        return rewards;
    }
}
