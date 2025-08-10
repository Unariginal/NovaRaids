package me.unariginal.novaraids.data;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public record Location(String id,
                       String name,
                       Vec3d pos,
                       ServerWorld world,
                       int borderRadius,
                       int bossPushbackRadius,
                       float bossFacingDirection,
                       boolean useSetJoinLocation,
                       Vec3d joinLocation,
                       float yaw,
                       float pitch) {
}
