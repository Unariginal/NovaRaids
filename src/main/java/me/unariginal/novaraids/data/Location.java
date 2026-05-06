package me.unariginal.novaraids.data;

import me.unariginal.novaraids.NovaRaids;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public record Location(String id,
                       String name,
                       Vec3d pos,
                       String worldPath,
                       int borderRadius,
                       int bossPushbackRadius,
                       float bossFacingDirection,
                       boolean useSetJoinLocation,
                       Vec3d joinLocation,
                       float yaw,
                       float pitch) {

    public ServerWorld world() {
        NovaRaids nr = NovaRaids.INSTANCE;
        for (ServerWorld w : nr.server().getWorlds()) {
            String id = w.getRegistryKey().getValue().toString();
            String path = w.getRegistryKey().getValue().getPath();
            if (id.equals(worldPath) || path.equals(worldPath)) {
                return w;
            }
        }
        nr.logError("World " + worldPath + " not found. Using overworld.");
        return nr.server().getOverworld();
    }

    public boolean isPointInLocation(double x, double z) {
        return Math.pow(x - pos.x, 2) + Math.pow(z - pos.z, 2) <= Math.pow(borderRadius, 2);
    }
}
