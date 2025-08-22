package me.unariginal.novaraids.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.Location;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LocationsConfig {
    private final NovaRaids nr = NovaRaids.INSTANCE;

    public List<Location> locations = new ArrayList<>();

    public LocationsConfig() {
        try {
            loadLocations();
        } catch (IOException | NullPointerException | UnsupportedOperationException e) {
            NovaRaids.LOADED = false;
            NovaRaids.LOGGER.error("[NovaRaids] Failed to load locations file.", e);
        }
    }

    public void loadLocations() throws IOException, NullPointerException, UnsupportedOperationException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids").toFile();
        if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }

        File file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/locations.json").toFile();

        JsonObject root = new JsonObject();
        if (file.exists()) root = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();

        if (root.keySet().isEmpty()) {
            JsonObject exampleLocationObject = new JsonObject();
            exampleLocationObject.addProperty("name", "Example Location");
            root.add("example_location", exampleLocationObject);
        }

        locations.clear();
        for (String key : root.keySet()) {
            JsonObject locationObject = root.getAsJsonObject(key);
            String name = key;
            double x = 0, y = 100, z = 0;
            ServerWorld world = nr.server().getOverworld();
            int borderRadius = 30;
            int bossPushbackRadius = 5;
            float bossFacingDirection = 0;
            boolean useJoinLocation = false;
            double joinX = 0;
            double joinY = 100;
            double joinZ = 0;
            float yaw = 0;
            float pitch = 0;

            if (locationObject.has("x_pos"))
                x = locationObject.get("x_pos").getAsDouble();
            locationObject.remove("x_pos");
            locationObject.addProperty("x_pos", x);

            if (locationObject.has("y_pos"))
                y = locationObject.get("y_pos").getAsDouble();
            locationObject.remove("y_pos");
            locationObject.addProperty("y_pos", y);

            if (locationObject.has("z_pos"))
                z = locationObject.get("z_pos").getAsDouble();
            locationObject.remove("z_pos");
            locationObject.addProperty("z_pos", z);

            Vec3d pos = new Vec3d(x, y, z);

            if (locationObject.has("world")) {
                String worldPath = locationObject.get("world").getAsString();
                boolean found = false;
                for (ServerWorld w : nr.server().getWorlds()) {
                    String id = w.getRegistryKey().getValue().toString();
                    String path = w.getRegistryKey().getValue().getPath();
                    if (id.equals(worldPath) || path.equals(worldPath)) {
                        world = w;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    nr.logError("World " + worldPath + " not found. Using overworld.");
                }
            }
            locationObject.remove("world");
            locationObject.addProperty("world", world.getRegistryKey().getValue().toString());

            if (locationObject.has("name"))
                name = locationObject.get("name").getAsString();
            locationObject.remove("name");
            locationObject.addProperty("name", name);

            if (locationObject.has("border_radius"))
                borderRadius = locationObject.get("border_radius").getAsInt();
            locationObject.remove("border_radius");
            locationObject.addProperty("border_radius", borderRadius);

            if (locationObject.has("boss_pushback_radius"))
                bossPushbackRadius = locationObject.get("boss_pushback_radius").getAsInt();
            locationObject.remove("boss_pushback_radius");
            locationObject.addProperty("boss_pushback_radius", bossPushbackRadius);

            if (locationObject.has("boss_facing_direction"))
                bossFacingDirection = locationObject.get("boss_facing_direction").getAsFloat();
            locationObject.remove("boss_facing_direction");
            locationObject.addProperty("boss_facing_direction", bossFacingDirection);

            if (locationObject.has("use_join_location"))
                useJoinLocation = locationObject.get("use_join_location").getAsBoolean();
            locationObject.remove("use_join_location");
            locationObject.addProperty("use_join_location", useJoinLocation);

            JsonObject joinLocationObject = new JsonObject();
            if (locationObject.has("join_location"))
                joinLocationObject = locationObject.get("join_location").getAsJsonObject();

            if (joinLocationObject.has("x_pos"))
                joinX = joinLocationObject.get("x_pos").getAsDouble();
            joinLocationObject.remove("x_pos");
            joinLocationObject.addProperty("x_pos", joinX);

            if (joinLocationObject.has("y_pos"))
                joinY = joinLocationObject.get("y_pos").getAsDouble();
            joinLocationObject.remove("y_pos");
            joinLocationObject.addProperty("y_pos", joinY);

            if (joinLocationObject.has("z_pos"))
                joinZ = joinLocationObject.get("z_pos").getAsDouble();
            joinLocationObject.remove("z_pos");
            joinLocationObject.addProperty("z_pos", joinZ);

            if (joinLocationObject.has("yaw"))
                yaw = joinLocationObject.get("yaw").getAsFloat();
            joinLocationObject.remove("yaw");
            joinLocationObject.addProperty("yaw", yaw);

            if (joinLocationObject.has("pitch"))
                pitch = joinLocationObject.get("pitch").getAsFloat();
            joinLocationObject.remove("pitch");
            joinLocationObject.addProperty("pitch", pitch);

            locationObject.remove("join_location");
            locationObject.add("join_location", joinLocationObject);

            Vec3d join_pos = new Vec3d(joinX, joinY, joinZ);
            locations.add(new Location(key, name, pos, world, borderRadius, bossPushbackRadius, bossFacingDirection, useJoinLocation, join_pos, yaw, pitch));
        }

        for (Location location : locations) {
            root.remove(location.id());

            JsonObject locationObject = new JsonObject();
            locationObject.addProperty("x_pos", location.pos().getX());
            locationObject.addProperty("y_pos", location.pos().getY());
            locationObject.addProperty("z_pos", location.pos().getZ());
            locationObject.addProperty("world", location.world().getRegistryKey().getValue().toString());
            locationObject.addProperty("name", location.name());
            locationObject.addProperty("border_radius", location.borderRadius());
            locationObject.addProperty("boss_pushback_radius", location.bossPushbackRadius());
            locationObject.addProperty("boss_facing_direction", location.bossFacingDirection());
            locationObject.addProperty("use_join_location", location.useSetJoinLocation());
            JsonObject joinLocationObject = new JsonObject();
            joinLocationObject.addProperty("x_pos", location.joinLocation().getX());
            joinLocationObject.addProperty("y_pos", location.joinLocation().getY());
            joinLocationObject.addProperty("z_pos", location.joinLocation().getZ());
            joinLocationObject.addProperty("yaw", location.yaw());
            joinLocationObject.addProperty("pitch", location.pitch());
            locationObject.add("join_location", joinLocationObject);

            root.add(location.id(), locationObject);
        }

        file.delete();
        file.createNewFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Writer writer = new FileWriter(file);
        gson.toJson(root, writer);
        writer.close();
    }

    public Location getLocation(String key) {
        for (Location loc : locations) {
            if (loc.id().equalsIgnoreCase(key)) {
                return loc;
            }
        }
        return null;
    }
}