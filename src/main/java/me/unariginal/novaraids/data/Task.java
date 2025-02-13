package me.unariginal.novaraids.data;

import net.minecraft.server.world.ServerWorld;

public record Task(ServerWorld world, Long execute_tick, Runnable action) {
}
