package me.unariginal.novaraids.data;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public record Location(String name, Vec3d pos, ServerWorld world) {
}
