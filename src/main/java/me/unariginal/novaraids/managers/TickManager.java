package me.unariginal.novaraids.managers;

import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.Task;
import me.unariginal.novaraids.utils.TextUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;

public class TickManager {
    private static final NovaRaids nr = NovaRaids.INSTANCE;

    public static void fix_boss_positions() {
        for (Raid raid : nr.active_raids().values()) {
            raid.fixBossPosition();
        }
    }

    public static void fix_player_positions() {
        for (Raid raid : nr.active_raids().values()) {
            for (ServerPlayerEntity player : raid.participating_players()) {
                int raid_radius = nr.config().getSettings().raid_radius();
                int raid_pushback = nr.config().getSettings().raid_pushback_radius();

                double x = player.getPos().getX();
                double z = player.getPos().getZ();
                double cx = raid.raidBoss_location().pos().getX();
                double cz = raid.raidBoss_location().pos().getZ();


                // Get direction vector
                double deltaX = x - cx;
                double deltaZ = z - cz;

                // Get angle of approach
                double angle = Math.toDegrees(Math.atan2(deltaZ, deltaX));

                // Modify based on quadrant
                if (angle < 0) {
                    angle += 360;
                }

                double distance = Double.NaN;

                double hyp = Math.pow(deltaX, 2) + Math.pow(deltaZ, 2);
                hyp = Math.sqrt(hyp);

                if (hyp < raid_pushback) {
                    distance =  raid_pushback + 1;
                } else if (hyp > raid_radius) {
                    distance = raid_radius - 1;
                }

                if (!Double.isNaN(distance)) {
                    double new_x = cx + distance * Math.cos(Math.toRadians(angle));
                    double new_z = cz + distance * Math.sin(Math.toRadians(angle));

                    player.teleport(new_x, raid.raidBoss_location().pos().getY(), new_z, false);
                }
            }
        }
    }

    public static void handle_defeated_bosses() {
        List<Raid> to_remove = new ArrayList<>();
        for (Raid raid : nr.active_raids().values()) {
            if (raid.stage() == -1) {
                to_remove.add(raid);
                continue;
            }

            if (raid.stage() == 2) {
                if (raid.current_health() <= 0) {
                    if (raid.boss_info().do_catch_phase()) {
                        raid.pre_catch_phase();
                    } else {
                        raid.participating_broadcast(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("boss_defeated"), raid)));
                        raid.raid_won();
                    }
                }
            }
        }

        for (Raid raid : to_remove) {
            nr.remove_raid(raid);
            raid.stop();
        }
    }

    public static void execute_tasks() {
        ServerWorld world = nr.server().getOverworld();
        long current_tick = world.getTime();
        for (Raid raid : nr.active_raids().values()) {
            if (!raid.getTasks().isEmpty()) {
                if (raid.getTasks().get(current_tick) != null) {
                    if (!raid.getTasks().get(current_tick).isEmpty()) {
                        for (Task task : raid.getTasks().get(current_tick)) {
                            task.action().run();
                        }
                        raid.getTasks().remove(current_tick);
                    }
                }
            }
        }
    }

    public static void update_bossbars() {
        for (Raid raid : nr.active_raids().values()) {
            if (raid.stage() == 2) {
                float progress = (float) raid.current_health() / raid.max_health();
                if (progress < 0) {
                    progress = 0;
                }
                for (ServerPlayerEntity player : raid.bossbars().keySet()) {
                    if (player != null) {
                        raid.bossbars().get(player).progress(progress);
                    }
                }
            } else {
                for (ServerPlayerEntity player : raid.bossbars().keySet()) {
                    float remaining_ticks = (float) (raid.phase_end_time() - nr.server().getOverworld().getTime());
                    float progress = 1.0F / (raid.phase_length() * 20L);
                    if (player != null) {
                        raid.bossbars().get(player).progress(progress * remaining_ticks);
                    }
                }
            }

            raid.show_overlay(raid.bossbar_data());
        }
    }
}
