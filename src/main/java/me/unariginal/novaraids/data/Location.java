package me.unariginal.novaraids.data;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public record Location(String id,
                       String name,
                       Vec3d pos,
                       ServerWorld world,
                       int border_radius,
                       int boss_pushback_radius,
                       float boss_facing_direction,
                       boolean use_set_join_location,
                       Vec3d join_location,
                       float yaw,
                       float pitch) {
}
