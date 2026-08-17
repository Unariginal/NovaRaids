package me.unariginal.novaraids.config;

import me.unariginal.novaraids.NovaRaids;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class LocationConfig {
    public transient String locationId;
    public String name;
    public DirectionalLocation bossLocation;
    public String world;
    public int borderRadius;
    public int bossPushbackRadius;
    public boolean useJoinLocation;
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

    public static LocationConfig getLocation(String id) {
        return ConfigManager.LOCATIONS.get(id);
    }

    public ServerWorld getServerWorld() {
        ServerWorld returnWorld = NovaRaids.INSTANCE.server.getOverworld();
        for (ServerWorld world : NovaRaids.INSTANCE.server.getWorlds()) {
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

    public List<ChunkPos> getChunks() {
        int minChunkX = Math.floorDiv((int) Math.floor(bossLocation.xPos - borderRadius), 16);
        int maxChunkX = Math.floorDiv((int) Math.floor(bossLocation.xPos + borderRadius), 16);

        int minChunkZ = Math.floorDiv((int) Math.floor(bossLocation.zPos - borderRadius), 16);
        int maxChunkZ = Math.floorDiv((int) Math.floor(bossLocation.zPos + borderRadius), 16);

        List<ChunkPos> chunks = new ArrayList<>();

        double radiusSq = borderRadius * borderRadius;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                double minX = chunkX * 16;
                double maxX = minX + 16;
                double minZ = chunkZ * 16;
                double maxZ = minZ + 16;

                double closestX = Math.clamp(bossLocation.xPos, minX, maxX);
                double closestZ = Math.clamp(bossLocation.zPos, minZ, maxZ);

                double dx = closestX - bossLocation.xPos;
                double dz = closestZ - bossLocation.zPos;

                if (dx * dx + dz * dz <= radiusSq) {
                    chunks.add(new ChunkPos(chunkX, chunkZ));
                }
            }
        }

        return chunks;
    }

    public void setChunksLoaded(boolean loaded) {
        for (ChunkPos chunkPos : getChunks()) {
            NovaRaids.INSTANCE.server.execute(() -> getServerWorld().setChunkForced(chunkPos.x, chunkPos.z, loaded));
        }
    }
}