package me.unariginal.novaraids.managers;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.Task;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TickManager {
    private static final NovaRaids nr = NovaRaids.INSTANCE;

    public static void fix_boss_positions() {
        for (Raid raid : nr.active_raids().values()) {
            raid.fixBossPosition();
            if (raid.stage() == 2 || raid.stage() == 4) {
                for (PokemonEntity pokemonEntity : raid.get_clones().keySet()) {
                    UUID player_uuid = raid.get_clones().get(pokemonEntity);
                    ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(player_uuid);
                    if (player != null) {
                        PokemonBattle battle = BattleRegistry.INSTANCE.getBattleByParticipatingPlayer(player);
                        if (battle == null) {
                            raid.remove_clone(pokemonEntity);
                        }
                    } else {
                        raid.remove_clone(pokemonEntity);
                    }
                }
            }

            if (raid.stage() == 4) {
                raid.raidBoss_entity().addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, -1, 9999, false, false));
                raid.raidBoss_entity().setGlowing(false);
            }

            if (raid.stage() > 1 && raid.participating_players().isEmpty()) {
                raid.stop();
            }
        }
    }

    public static void fix_player_positions() {
        for (Raid raid : nr.active_raids().values()) {
            for (UUID player_uuid : raid.participating_players()) {
                ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(player_uuid);
                if (player != null) {
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
                        distance = raid_pushback + 1;
                    } else if (hyp > raid_radius) {
                        if (raid.stage() < 3) {
                            distance = raid_radius - 1;
                        }
                    }

                    if (!Double.isNaN(distance)) {
                        double new_x = cx + distance * Math.cos(Math.toRadians(angle));
                        double new_z = cz + distance * Math.sin(Math.toRadians(angle));

                        player.teleport(new_x, raid.raidBoss_location().pos().getY(), new_z, false);
                    }
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
                    raid.pre_catch_phase();
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
                for (UUID player_uuid : raid.bossbars().keySet()) {
                    ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(player_uuid);
                    if (player != null) {
                        raid.bossbars().get(player.getUuid()).progress(progress);
                    }
                }
            } else {
                for (UUID player_uuid : raid.bossbars().keySet()) {
                    float remaining_ticks = (float) (raid.phase_end_time() - nr.server().getOverworld().getTime());
                    float progress = 1.0F / (raid.phase_length() * 20L);
                    raid.bossbars().get(player_uuid).progress(progress * remaining_ticks);
                }
            }

            raid.show_overlay(raid.bossbar_data());
        }
    }

    public static void fix_player_pokemon() {
        for (Raid raid : nr.active_raids().values()) {
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

                                            double distance = nr.config().getSettings().raid_radius() + 2;

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
