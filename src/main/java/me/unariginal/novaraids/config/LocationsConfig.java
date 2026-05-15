package me.unariginal.novaraids.config;

import me.unariginal.novaraids.NovaRaids;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class LocationsConfig {
    public String name;
    public DirectionalLocation bossLocation;
    public String world;
    public int borderRadius;
    public int bossPushbackRadius;
    public boolean useSetJoinLocation;
    public DirectionalLocation joinLocation;

    public static class DirectionalLocation {
        public double xPos = 0.0;
        public double yPos = 64.0;
        public double zPos = 0.0;
        public float yaw = 0.0f;
        public float pitch = 0.0f;

        public Vec3d getPos() {
            return new Vec3d(xPos, yPos, zPos);
        }
    }

    public static LocationsConfig getLocation(String id) {
        return ConfigManager.LOCATIONS.get(id);
    }

    public ServerWorld getServerWorld() {
        ServerWorld returnWorld = NovaRaids.INSTANCE.server().getOverworld();
        for (ServerWorld world : NovaRaids.INSTANCE.server().getWorlds()) {
            String id = world.getRegistryKey().getValue().toString();
            String path = world.getRegistryKey().getValue().getPath();
            if (id.equals(this.world) || path.equals(this.world)) {
                returnWorld = world;
                break;
            }
        }
        return returnWorld;
    }

    public boolean isPointInLocation(double x, double z) {
        return Math.pow(x - bossLocation.xPos, 2) + Math.pow(z - bossLocation.zPos, 2) <= Math.pow(borderRadius, 2);
    }
}