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
import net.minecraft.util.math.Vec3d;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class TickManager {
    private static final NovaRaids nr = NovaRaids.INSTANCE;
    private static ZonedDateTime setTimeBuffer = ZonedDateTime.now(nr.schedulesConfig().zone);
    private static int webhookUpdateTimer = WebhookHandler.webhookUpdateRateSeconds * 20;

    public static void updateWebhooks() {
        webhookUpdateTimer--;
        if (webhookUpdateTimer <= 0) {
            webhookUpdateTimer = WebhookHandler.webhookUpdateRateSeconds * 20;

            Collection<Raid> raids = nr.activeRaids().values();
            for (Raid raid : raids) {
                long id = raid.getCurrentWebhookID();
                if (id == 0) {
                    continue;
                }
                if (raid.stage() == 1) {
                    WebhookHandler.editStartRaidWebhook(id, raid);
                } else if (raid.stage() == 2) {
                    WebhookHandler.editRunningWebhook(id, raid);
                }
            }
        }
    }

    public static void fixBossPositions() {
        Collection<Raid> raids = nr.activeRaids().values();
        for (Raid raid : raids) {
            raid.fixBossPosition();
            Collection<PokemonEntity> clones = raid.getClones().keySet();
            if (raid.stage() == 2 || raid.stage() == 4) {
                List<PokemonEntity> toRemove = new ArrayList<>();
                for (PokemonEntity pokemonEntity : clones) {
                    UUID playerUUID = raid.getClones().get(pokemonEntity);
                    ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(playerUUID);
                    if (player != null) {
                        PokemonBattle battle = BattleRegistry.getBattleByParticipatingPlayer(player);
                        if (battle == null) {
                            toRemove.add(pokemonEntity);
                        }
                    } else {
                        toRemove.add(pokemonEntity);
                    }
                }
                for (PokemonEntity pokemonEntity : toRemove) {
                    raid.removeClone(pokemonEntity, false);
                }
            }

            if (raid.stage() > 1 && raid.participatingPlayers().isEmpty()) {
                raid.stop();
            }
        }
    }

    public static void fixPlayerPositions() {
        Collection<Raid> raids = nr.activeRaids().values();
        Collection<ServerPlayerEntity> players = nr.server().getPlayerManager().getPlayerList();
        for (Raid raid : raids) {
            for (ServerPlayerEntity player : players) {
                if (player != null) {
                    int raidRadius = raid.raidBossLocation().borderRadius();
                    int raidPushback = raid.raidBossLocation().bossPushbackRadius();
                    ServerWorld world = raid.raidBossLocation().world();
                    if (player.getServerWorld() == world) {
                        double x = player.getPos().getX();
                        double z = player.getPos().getZ();
                        double cx = raid.raidBossLocation().pos().getX();
                        double cz = raid.raidBossLocation().pos().getZ();

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

                        if (hyp < raidPushback) {
                            distance = raidPushback + 1;
                        } else if (hyp > raidRadius) {
                            if (raid.stage() < 3 && raid.participatingPlayers().contains(player.getUuid()) && raid.stage() != -1) {
                                distance = raidRadius - 1;
                            }
                        }

                        if (!Double.isNaN(distance)) {
                            double newX = cx + distance * Math.cos(Math.toRadians(angle));
                            double newZ = cz + distance * Math.sin(Math.toRadians(angle));
                            double newY = raid.raidBossLocation().pos().getY();

                            while (!world.getBlockState(new BlockPos((int) newX, (int) newY, (int) newZ)).isAir()) {
                                newY++;
                            }
                            player.teleport(world, newX, newY, newZ, player.getYaw(), player.getPitch());
                        }
                    }
                }
            }
        }
    }

    public static void handleDefeatedBosses() {
        List<Raid> toRemove = new ArrayList<>();
        Collection<Raid> raids = nr.activeRaids().values();
        for (Raid raid : raids) {
            if (raid.stage() == -1) {
                toRemove.add(raid);
                continue;
            }

            if (raid.stage() == 2) {
                if (raid.currentHealth() <= 0) {
                    raid.preCatchPhase();
                }
            }
        }

        for (Raid raid : toRemove) {
            nr.removeRaid(raid);
            raid.stop();
        }
    }

    public static void executeTasks() {
        ServerWorld world = nr.server().getOverworld();
        long currentTick = world.getTime();
        Collection<Raid> raids = nr.activeRaids().values();
        for (Raid raid : raids) {
            if (!raid.getTasks().isEmpty()) {
                if (raid.getTasks().get(currentTick) != null) {
                    if (!raid.getTasks().get(currentTick).isEmpty()) {
                        for (Task task : raid.getTasks().get(currentTick)) {
                            task.action().run();
                        }
                        raid.getTasks().remove(currentTick);
                    }
                }
            }
        }
    }

    public static void fixPlayerPokemon() {
        if (nr.config().hideOtherPokemonInRaid) return;

        Collection<Raid> raids = nr.activeRaids().values(); // Prevents CME

        for (Raid raid : raids) {
            Collection<UUID> participatingUUIDs = raid.participatingPlayers();
            if (raid.stage() == 2) {
                for (UUID playerUUID : participatingUUIDs) {

                    ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(playerUUID);
                    if (player == null) return;

                    PokemonBattle battle = BattleRegistry.getBattleByParticipatingPlayer(player);
                    if (battle == null) return;

                    BattleActor actor = battle.getActor(player);
                    if (actor == null) return;

                    if (actor.getActivePokemon().isEmpty()) return;

                    // Moved up - don't get/calculate for every pokemon
                    Vec3d bossPos = raid.raidBossLocation().pos();
                    double cx = bossPos.getX();
                    double cz = bossPos.getZ();

                    double distance = Math.min(30, raid.raidBossLocation().borderRadius());

                    for (ActiveBattlePokemon activeBattlePokemon : actor.getActivePokemon()) {
                        BattlePokemon battlePokemon = activeBattlePokemon.getBattlePokemon();
                        if (battlePokemon == null) continue;

                        PokemonEntity entity = battlePokemon.getEntity();
                        if (entity == null) return;

                        // Split into its own method for clarity
                        double angle = getAngle(entity, cx, cz);

                        // Prevents us converting back here...
                        double newX = cx + distance * Math.cos(angle);
                        double newZ = cz + distance * Math.sin(angle);

                        entity.setPosition(newX, raid.raidBossLocation().pos().getY(), newZ);
                    }
                }
            }
        }
    }

    private static double getAngle(PokemonEntity entity, double cx, double cz) {
        double x = entity.getPos().getX();
        double z = entity.getPos().getZ();

        // Get direction vector
        double deltaX = x - cx;
        double deltaZ = z - cz;

        // Get angle of approach
        // Change - We convert straight back to radians so no need to convert to degrees first. atan2 works fine with -ve radians
        return Math.atan2(deltaZ, deltaX);
    }

    public static void scheduledRaids() {
        ZonedDateTime now = ZonedDateTime.now(nr.schedulesConfig().zone);
        Collection<Schedule> schedules = nr.schedulesConfig().schedules;
        for (Schedule schedule : schedules) {
            boolean shouldExecute = false;
            if (schedule instanceof SpecificSchedule specificSchedule) {
                if (setTimeBuffer.until(now, ChronoUnit.SECONDS) >= 1 && specificSchedule.isNextTime()) {
                    nr.logInfo("Attempting scheduled raid");
                    setTimeBuffer = now;
                    shouldExecute = true;
                }
            } else if (schedule instanceof RandomSchedule randomSchedule) {
                if (setTimeBuffer.until(now, ChronoUnit.SECONDS) >= 1 && randomSchedule.isNextTime()) {
                    nr.logInfo("Attempting random raid");
                    setTimeBuffer = now;
                    randomSchedule.setNextRandom(now);
                    shouldExecute = true;
                }
            } else if (schedule instanceof CronSchedule cronSchedule) {
                if (setTimeBuffer.until(now, ChronoUnit.SECONDS) >= 1 && cronSchedule.isNextTime()) {
                    nr.logInfo("Attempting cron raid");
                    setTimeBuffer = now;
                    cronSchedule.setNextExecution(now);
                    shouldExecute = true;
                }
            }

            if (shouldExecute) {
                double totalWeight = 0.0;
                for (ScheduleBoss scheduleBoss : schedule.bosses) {
                    if (scheduleBoss.type().equalsIgnoreCase("category")) {
                        if (nr.bossesConfig().getCategory(scheduleBoss.id()) != null) {
                            totalWeight += scheduleBoss.weight();
                        } else {
                            nr.logError("Category " + scheduleBoss.id() + " does not exist. Skipping.");
                        }
                    } else if (scheduleBoss.type().equalsIgnoreCase("boss")) {
                        if (nr.bossesConfig().getBoss(scheduleBoss.id()) != null) {
                            totalWeight += scheduleBoss.weight();
                        } else {
                            nr.logError("Boss " + scheduleBoss.id() + " does not exist. Skipping.");
                        }
                    }
                }
                if (totalWeight > 0.0) {
                    double randomWeight = new Random().nextDouble(totalWeight);
                    totalWeight = 0.0;
                    for (ScheduleBoss scheduleBoss : schedule.bosses) {
                        if (scheduleBoss.type().equalsIgnoreCase("category")) {
                            Category category = nr.bossesConfig().getCategory(scheduleBoss.id());
                            if (category != null) {
                                totalWeight += scheduleBoss.weight();
                                if (randomWeight < totalWeight) {
                                    Boss boss = nr.bossesConfig().getRandomBoss(category.id(), scheduleBoss.blacklistedBosses());
                                    if (boss != null) {
                                        nr.raidCommands().start(boss, null, null);
                                        break;
                                    } else {
                                        nr.logError("Failed to start scheduled raid. Boss was null!");
                                    }
                                }
                            }
                        } else if (scheduleBoss.type().equalsIgnoreCase("boss")) {
                            Boss boss = nr.bossesConfig().getBoss(scheduleBoss.id());
                            if (boss != null) {
                                totalWeight += scheduleBoss.weight();
                                if (randomWeight < totalWeight) {
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
