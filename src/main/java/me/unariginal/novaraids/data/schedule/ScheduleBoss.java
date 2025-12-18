package me.unariginal.novaraids.data.schedule;

import java.util.List;

public record ScheduleBoss(String type, String id, double weight, List<String> blacklistedBosses) {
}
