package me.unariginal.novaraids.managers;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.Category;
import me.unariginal.novaraids.data.Task;
import me.unariginal.novaraids.data.bosssettings.Boss;
import me.unariginal.novaraids.data.schedule.*;
import me.unariginal.novaraids.utils.WebhookHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class TickManager {
    private static final NovaRaids nr = NovaRaids.INSTANCE;
    private static ZonedDateTime set_time_buffer = ZonedDateTime.now(nr.schedulesConfig().zone);
    private static int webhook_update_timer = WebhookHandler.webhook_update_rate_seconds * 20;

    public static void update_webhooks() throws ConcurrentModificationException {
        webhook_update_timer--;
        if (webhook_update_timer <= 0) {
            webhook_update_timer = WebhookHandler.webhook_update_rate_seconds * 20;

//            if (!nr.config().opt_out) {
//                try {
//                    CollectingDataToSellToTheChineseGovernment.editStartWebhook();
//                } catch (ExecutionException | InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }

            for (Raid raid : nr.active_raids().values()) {
                long id = raid.getCurrentWebhookID();
                if (id == 0) {
                    continue;
                }
                if (raid.stage() == 1) {
                    try {
                        WebhookHandler.editStartRaidWebhook(id, raid);
                    } catch (ExecutionException | InterruptedException e) {
                        nr.logError("Failed to edit raid_start webhook: " + e.getMessage());
                    }
                } else if (raid.stage() == 2) {
                    try {
                        WebhookHandler.editRunningWebhook(id, raid);
                    } catch (ExecutionException | InterruptedException e) {
                        nr.logError("Failed to edit raid_running webhook: " + e.getMessage());
                    }
                }
            }
        }
    }

    public static void fix_boss_positions() throws ConcurrentModificationException {
        for (Raid raid : nr.active_raids().values()) {
            raid.fixBossPosition();
            if (raid.stage() == 2 || raid.stage() == 4) {
                List<PokemonEntity> toRemove = new ArrayList<>();
                for (PokemonEntity pokemonEntity : raid.get_clones().keySet()) {
                    UUID player_uuid = raid.get_clones().get(pokemonEntity);
                    ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(player_uuid);
                    if (player != null) {
                        PokemonBattle battle = BattleRegistry.INSTANCE.getBattleByParticipatingPlayer(player);
                        if (battle == null) {
                            toRemove.add(pokemonEntity);
                        }
                    } else {
                        toRemove.add(pokemonEntity);
                    }
                }
                for (PokemonEntity pokemonEntity : toRemove) {
                    raid.remove_clone(pokemonEntity);
                }
            }

            if (raid.stage() > 1 && raid.participating_players().isEmpty()) {
                raid.stop();
            }
        }
    }

    public static void fix_player_positions() throws ConcurrentModificationException {
        for (Raid raid : nr.active_raids().values()) {
            for (ServerPlayerEntity player : nr.server().getPlayerManager().getPlayerList()) {
                if (player != null) {
                    int raid_radius = raid.raidBoss_location().border_radius();
                    int raid_pushback = raid.raidBoss_location().boss_pushback_radius();
                    ServerWorld world = raid.raidBoss_location().world();
                    if (player.getServerWorld() == world) {
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
                            distance = raid_pushback + 1;
                        } else if (hyp > raid_radius) {
                            if (raid.stage() < 3 && raid.participating_players().contains(player.getUuid()) && raid.stage() != -1 ) {
                                distance = raid_radius - 1;
                            }
                        }

                        if (!Double.isNaN(distance)) {
                            double new_x = cx + distance * Math.cos(Math.toRadians(angle));
                            double new_z = cz + distance * Math.sin(Math.toRadians(angle));
                            double new_y = raid.raidBoss_location().pos().getY();
                            int chunkX = (int) Math.floor(new_x / 16);
                            int chunkZ = (int) Math.floor(new_z / 16);
                            world.setChunkForced(chunkX, chunkZ, true);
                            while (!world.getBlockState(new BlockPos((int) new_x, (int) new_y, (int) new_z)).isAir()) {
                                new_y++;
                            }
                            player.teleport(world, new_x, new_y, new_z, player.getYaw(), player.getPitch());
                            world.setChunkForced(chunkX, chunkZ, false);
                        }
                    }
                }
            }
        }
    }

    public static void handle_defeated_bosses() throws ConcurrentModificationException {
        List<Raid> to_remove = new ArrayList<>();
        for (Raid raid : nr.active_raids().values()) {
            if (raid.stage() == -1) {
                to_remove.add(raid);
                continue;
            }

            if (raid.stage() == 2) {
                if (raid.current_health() <= 0) {
                    raid.pre_catch_phase();
                }
            }
        }

        for (Raid raid : to_remove) {
            nr.remove_raid(raid);
            raid.stop();
        }
    }

    public static void execute_tasks() throws ConcurrentModificationException {
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

    public static void update_bossbars() throws ConcurrentModificationException {
        for (Raid raid : nr.active_raids().values()) {
            if (raid.stage() == 2) {
                float progress = (float) raid.current_health() / raid.max_health();

                if (progress < 0F) {
                    progress = 0F;
                }

                if (progress > 1F) {
                    progress = 1F;
                }

                for (UUID player_uuid : raid.bossbars().keySet()) {
                    try {
                        raid.bossbars().get(player_uuid).progress(progress);
                    } catch (IllegalArgumentException | NullPointerException e) {
                        nr.logError("Error updating bossbar for player uuid: " + player_uuid);
                        nr.logError("Error Message: " + e.getMessage());
                    }
                }
            } else {
                float remaining_ticks = (float) (raid.phase_end_time() - nr.server().getOverworld().getTime());
                float progress = 1.0F / (raid.phase_length() * 20L);
                float total = progress * remaining_ticks;

                if (total < 0F) {
                    total = 0F;
                }

                if (total > 1F) {
                    total = 1F;
                }

                for (UUID player_uuid : raid.bossbars().keySet()) {
                    try {
                        if (raid.bossbars().containsKey(player_uuid) && raid.bossbars().get(player_uuid) != null) {
                            raid.bossbars().get(player_uuid).progress(total);
                        }
                    } catch (IllegalArgumentException | NullPointerException e) {
                        nr.logError("Error updating bossbar for player uuid: " + player_uuid);
                        nr.logError("Error Message: " + e.getMessage());
                    }
                }
            }

            raid.show_overlay(raid.bossbar_data());
        }
    }

    public static void fix_player_pokemon() throws ConcurrentModificationException {
        if (!nr.config().hide_other_pokemon_in_raid) {
            for (Raid raid : nr.active_raids().values()) {
                if (raid.stage() == 2) {
                    for (UUID player_uuid : raid.participating_players()) {
                        ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(player_uuid);
                        if (player != null) {
                            PokemonBattle battle = BattleRegistry.INSTANCE.getBattleByParticipatingPlayer(player);
                            if (battle != null) {
                                BattleActor actor = battle.getActor(player);
                                if (actor != null) {
                                    if (!actor.getActivePokemon().isEmpty()) {
                                        for (ActiveBattlePokemon activeBattlePokemon : actor.getActivePokemon()) {
                                            BattlePokemon battlePokemon = activeBattlePokemon.getBattlePokemon();
                                            if (battlePokemon != null) {
                                                PokemonEntity entity = battlePokemon.getEntity();
                                                if (entity != null) {
                                                    double x = entity.getPos().getX();
                                                    double z = entity.getPos().getZ();
                                                    double cx = raid.raidBoss_location().pos().getX();
                                                    double cz = raid.raidBoss_location().pos().getZ();

                                                    // Get direction vector
                                                    double deltaX = x - cx;
                                                    double deltaZ = z - cz;

                                                    // Get angle of approach
                                                    double angle = Math.toDegrees(Math.atan2(deltaZ, deltaX));

                                                    if (angle < 0) {
                                                        angle += 360;
                                                    }

                                                    double distance = 30;

                                                    double new_x = cx + distance * Math.cos(Math.toRadians(angle));
                                                    double new_z = cz + distance * Math.sin(Math.toRadians(angle));

                                                    entity.setPosition(new_x, raid.raidBoss_location().pos().getY(), new_z);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void scheduled_raids() throws ConcurrentModificationException {
        ZonedDateTime now = ZonedDateTime.now(nr.schedulesConfig().zone);
        for (Schedule schedule : nr.schedulesConfig().schedules) {
            boolean shouldExecute = false;
            if (schedule instanceof SpecificSchedule specificSchedule) {
                if (set_time_buffer.until(now, ChronoUnit.SECONDS) >= 1 && specificSchedule.isNextTime()) {
                    nr.logInfo("[RAIDS] Attempting scheduled raid");
                    set_time_buffer = now;
                    shouldExecute = true;
                }
            } else if (schedule instanceof RandomSchedule randomSchedule) {
                if (set_time_buffer.until(now, ChronoUnit.SECONDS) >= 1 && randomSchedule.isNextTime()) {
                    nr.logInfo("[RAIDS] Attempting random raid");
                    set_time_buffer = now;
                    randomSchedule.setNextRandom(now);
                    shouldExecute = true;
                }
            } else if (schedule instanceof CronSchedule cronSchedule) {
                if (set_time_buffer.until(now, ChronoUnit.SECONDS) >= 1 && cronSchedule.isNextTime()) {
                    nr.logInfo("[RAIDS] Attempting cron raid");
                    set_time_buffer = now;
                    cronSchedule.setNextExecution(now);
                    shouldExecute = true;
                }
            }

            if (shouldExecute) {
                double total_weight = 0.0;
                for (ScheduleBoss scheduleBoss : schedule.bosses) {
                    if (scheduleBoss.type().equalsIgnoreCase("category")) {
                        if (nr.bossesConfig().getCategory(scheduleBoss.id()) != null) {
                            total_weight += scheduleBoss.weight();
                        } else {
                            nr.logError("[RAIDS] Category " + scheduleBoss.id() + " does not exist. Skipping.");
                        }
                    } else if (scheduleBoss.type().equalsIgnoreCase("boss")) {
                        if (nr.bossesConfig().getBoss(scheduleBoss.id()) != null) {
                            total_weight += scheduleBoss.weight();
                        } else {
                            nr.logError("[RAIDS] Boss " + scheduleBoss.id() + " does not exist. Skipping.");
                        }
                    }
                }
                if (total_weight > 0.0) {
                    double random_weight = new Random().nextDouble(total_weight);
                    total_weight = 0.0;
                    for (ScheduleBoss scheduleBoss : schedule.bosses) {
                        if (scheduleBoss.type().equalsIgnoreCase("category")) {
                            Category category = nr.bossesConfig().getCategory(scheduleBoss.id());
                            if (category != null) {
                                total_weight += scheduleBoss.weight();
                                if (random_weight < total_weight) {
                                    Boss boss = nr.bossesConfig().getRandomBoss(category.id());
                                    if (boss != null) {
                                        nr.raidCommands().start(boss, null, null);
                                        break;
                                    } else {
                                        nr.logError("[RAIDS] Failed to start scheduled raid. Boss was null!");
                                    }
                                }
                            }
                        } else if (scheduleBoss.type().equalsIgnoreCase("boss")) {
                            Boss boss = nr.bossesConfig().getBoss(scheduleBoss.id());
                            if (boss != null) {
                                total_weight += scheduleBoss.weight();
                                if (random_weight < total_weight) {
                                    nr.raidCommands().start(boss, null, null);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
