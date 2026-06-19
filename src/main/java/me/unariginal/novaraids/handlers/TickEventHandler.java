package me.unariginal.novaraids.handlers;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.data.Task;
import me.unariginal.novaraids.data.schedules.*;
import me.unariginal.novaraids.events.RaidEvents;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.raid.RaidManager;
import me.unariginal.novaraids.raid.RaidPhase;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static me.unariginal.novaraids.NovaRaids.logError;
import static me.unariginal.novaraids.NovaRaids.logInfo;
import static me.unariginal.novaraids.config.ConfigManager.CONFIG;
import static me.unariginal.novaraids.config.ConfigManager.SCHEDULES;
import static me.unariginal.novaraids.raid.RaidManager.activeRaids;

public class TickEventHandler {
    private static final NovaRaids nr = NovaRaids.INSTANCE;
    private static ZonedDateTime setTimeBuffer = ZonedDateTime.now(SCHEDULES.getTimezone());
    private static int webhookUpdateTimer = CONFIG.discordWebhook.updateRateSeconds * 20;
    private static int raidStartTimer = 20;

    public static void attemptNextRaid() {
        raidStartTimer--;
        if (raidStartTimer <= 0) {
            raidStartTimer = 20;
            if (CONFIG.raidSettings.useQueueSystem) {
                if (activeRaids.isEmpty()) {
                    RaidManager.startNextQueuedRaid();
                }
            } else {
                RaidManager.startNextQueuedRaid();
            }
        }
    }

    public static void updateWebhooks() {
        if (CONFIG.discordWebhook.enabled) {
            webhookUpdateTimer--;
            if (webhookUpdateTimer <= 0) {
                webhookUpdateTimer = CONFIG.discordWebhook.updateRateSeconds * 20;

                Collection<Raid> raids = activeRaids.values();
                for (Raid raid : raids) {
                    if (raid.webhookID == 0 || raid.currentWebhookEvent == null) {
                        continue;
                    }
                    if (raid.phase == RaidPhase.SETUP || raid.phase == RaidPhase.FIGHT) {
                        WebhookHandler.editWebhookEmbed(raid.currentWebhookEvent, raid, raid.webhookDamage).thenAccept(id -> raid.webhookID = id);
                    }
                }
            }
        }
    }

    public static void fixBossPositions() {
        Collection<Raid> raids = activeRaids.values();
        for (Raid raid : raids) {
            raid.fixBossPosition();
            Collection<PokemonEntity> clones = raid.clones.keySet();
            if (raid.phase == RaidPhase.FIGHT || raid.phase == RaidPhase.CATCH) {
                List<PokemonEntity> toRemove = new ArrayList<>();
                for (PokemonEntity pokemonEntity : clones) {
                    boolean removable = false;
                    if (!raid.cloneRemovalBuffer.containsKey(pokemonEntity) || raid.cloneRemovalBuffer.get(pokemonEntity) <= 0) {
                        raid.cloneRemovalBuffer.remove(pokemonEntity);
                        removable = true;
                    } else if (raid.cloneRemovalBuffer.containsKey(pokemonEntity)) {
                        raid.cloneRemovalBuffer.put(pokemonEntity, raid.cloneRemovalBuffer.get(pokemonEntity) - 1);
                    }

                    UUID playerUUID = raid.clones.get(pokemonEntity);
                    ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(playerUUID);
                    if (player != null) {
                        PokemonBattle battle = BattleRegistry.getBattleByParticipatingPlayer(player);
                        if (battle == null && removable) {
                            logError("Battle is null. Setting clone to be removed.");
                            toRemove.add(pokemonEntity);
                        }
                    } else if (removable) {
                        logError("Player is null. Setting clone to be removed.");
                        toRemove.add(pokemonEntity);
                    }
                }
                for (PokemonEntity pokemonEntity : toRemove) {
                    raid.removeClone(pokemonEntity, false);
                }
            }

            if (raid.phase != RaidPhase.INIT && raid.phase != RaidPhase.SETUP && raid.phase != RaidPhase.STOPPING && raid.participatingPlayers.isEmpty()) {
                raid.stop();
            }
        }
    }

    public static void fixPlayerPositions() {
        Collection<Raid> raids = activeRaids.values();
        Collection<ServerPlayerEntity> players = nr.server.getPlayerManager().getPlayerList();
        for (Raid raid : raids) {
            if (raid.phase != RaidPhase.FIGHT && raid.phase != RaidPhase.SETUP) continue;
            for (ServerPlayerEntity player : players) {
                if (player != null) {
                    int raidRadius = raid.location.borderRadius;
                    int raidPushback = raid.location.bossPushbackRadius;
                    ServerWorld world = raid.location.getServerWorld();
                    if (player.getServerWorld() == world) {
                        double x = player.getPos().getX();
                        double z = player.getPos().getZ();
                        double cx = raid.location.bossLocation.xPos;
                        double cz = raid.location.bossLocation.zPos;

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
                            if ((raid.phase == RaidPhase.FIGHT || raid.phase == RaidPhase.SETUP) && raid.participatingPlayers.contains(player.getUuid())) {
                                distance = raidRadius - 1;
                            }
                        }

                        if (!Double.isNaN(distance)) {
                            double newX = cx + distance * Math.cos(Math.toRadians(angle));
                            double newZ = cz + distance * Math.sin(Math.toRadians(angle));
                            double newY = raid.location.bossLocation.yPos;

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
        Collection<Raid> raids = activeRaids.values();
        for (Raid raid : raids) {
            if (raid.phase == RaidPhase.STOPPING) {
                toRemove.add(raid);
                continue;
            }

            if (raid.phase == RaidPhase.FIGHT) {
                if (raid.currentHealth <= 0) {
                    RaidEvents.BOSS_DEFEATED_EVENT_PRE.invoker().onBossDefeatedPre(raid);
                    raid.preCatchPhase();
                    RaidEvents.BOSS_DEFEATED_EVENT_POST.invoker().onBossDefeatedPost(raid);
                }
            }
        }

        for (Raid raid : toRemove) {
            RaidManager.stopRaid(raid.uuid);
        }
    }

    public static void executeTasks() {
        ServerWorld world = nr.server.getOverworld();
        long currentTick = world.getTime();
        Collection<Raid> raids = activeRaids.values();
        for (Raid raid : raids) {
            if (!raid.tasks.isEmpty()) {
                if (raid.tasks.get(currentTick) != null) {
                    if (!raid.tasks.get(currentTick).isEmpty()) {
                        for (Task task : raid.tasks.get(currentTick)) {
                            task.action().run();
                        }
                        raid.tasks.remove(currentTick);
                    }
                }
            }
        }
    }

    public static void fixPlayerPokemon() {
        if (CONFIG.raidSettings.hideOtherPokemonInRaid) return;

        Collection<Raid> raids = activeRaids.values();

        for (Raid raid : raids) {
            Collection<UUID> participatingUUIDs = raid.participatingPlayers;
            if (raid.phase == RaidPhase.FIGHT) {
                for (UUID playerUUID : participatingUUIDs) {

                    ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(playerUUID);
                    if (player == null) return;

                    PokemonBattle battle = BattleRegistry.getBattleByParticipatingPlayer(player);
                    if (battle == null) return;

                    BattleActor actor = battle.getActor(player);
                    if (actor == null) return;

                    if (actor.getActivePokemon().isEmpty()) return;

                    // Moved up - don't get/calculate for every pokemon
                    Vec3d bossPos = raid.location.bossLocation.getPos();
                    double cx = bossPos.getX();
                    double cz = bossPos.getZ();

                    double distance = Math.min(30, raid.location.borderRadius);

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

                        entity.setPosition(newX, raid.location.bossLocation.yPos, newZ);
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
        ZonedDateTime now = ZonedDateTime.now(SCHEDULES.getTimezone());
        Collection<Schedule> schedules = SCHEDULES.schedules;
        for (Schedule schedule : schedules) {
            boolean shouldExecute = false;
            if (schedule instanceof SpecificSchedule specificSchedule) {
                if (setTimeBuffer.until(now, ChronoUnit.SECONDS) >= 1 && specificSchedule.isNextTime()) {
                    logInfo("Attempting scheduled raid");
                    setTimeBuffer = now;
                    shouldExecute = true;
                }
            } else if (schedule instanceof RandomSchedule randomSchedule) {
                if (setTimeBuffer.until(now, ChronoUnit.SECONDS) >= 1 && randomSchedule.isNextTime()) {
                    logInfo("Attempting random raid");
                    setTimeBuffer = now;
                    randomSchedule.setNextRandom(now);
                    shouldExecute = true;
                }
            } else if (schedule instanceof CronSchedule cronSchedule) {
                if (setTimeBuffer.until(now, ChronoUnit.SECONDS) >= 1 && cronSchedule.isNextTime()) {
                    logInfo("Attempting cron raid");
                    setTimeBuffer = now;
                    cronSchedule.setNextExecution(now);
                    shouldExecute = true;
                }
            }

            if (shouldExecute) {
                double totalWeight = 0.0;
                for (ScheduleSection scheduleSection : schedule.bosses) {
                    if (scheduleSection instanceof ScheduleCategory scheduleCategory) {
                        if (Category.getCategory(scheduleCategory.categoryId) != null) {
                            totalWeight += scheduleCategory.weight;
                        } else {
                            logError("Category " + scheduleCategory.categoryId + " does not exist. Skipping.");
                        }
                    } else if (scheduleSection instanceof ScheduleBoss scheduleBoss) {
                        if (Boss.getBoss(scheduleBoss.bossId) != null) {
                            totalWeight += scheduleBoss.weight;
                        } else {
                            logError("Boss " + scheduleBoss.bossId + " does not exist. Skipping.");
                        }
                    }
                }
                if (totalWeight > 0.0) {
                    double randomWeight = new Random().nextDouble(totalWeight);
                    totalWeight = 0.0;
                    for (ScheduleSection scheduleSection : schedule.bosses) {
                        if (scheduleSection instanceof ScheduleCategory scheduleCategory) {
                            Category category = Category.getCategory(scheduleCategory.categoryId);
                            if (category != null) {
                                totalWeight += scheduleCategory.weight;
                                if (randomWeight < totalWeight) {
                                    Boss boss = Boss.getRandomBoss(category.categoryId, scheduleCategory.blacklistedBosses);
                                    if (boss != null) {
                                        RaidManager.queueRaid(boss, null, null, null);
                                        break;
                                    } else {
                                        logError("Failed to start scheduled raid. Boss was null!");
                                    }
                                }
                            }
                        } else if (scheduleSection instanceof ScheduleBoss scheduleBoss) {
                            Boss boss = Boss.getBoss(scheduleBoss.bossId);
                            if (boss != null) {
                                totalWeight += scheduleBoss.weight;
                                if (randomWeight < totalWeight) {
                                    RaidManager.queueRaid(boss, null, null, null);
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
