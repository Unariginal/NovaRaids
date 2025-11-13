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
    private static ZonedDateTime setTimeBuffer = ZonedDateTime.now(nr.schedulesConfig().zone);
    private static int webhookUpdateTimer = WebhookHandler.webhookUpdateRateSeconds * 20;

    public static void updateWebhooks() throws ConcurrentModificationException {
        webhookUpdateTimer--;
        if (webhookUpdateTimer <= 0) {
            webhookUpdateTimer = WebhookHandler.webhookUpdateRateSeconds * 20;

            for (Raid raid : nr.activeRaids().values()) {
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

    public static void fixBossPositions() throws ConcurrentModificationException {
        for (Raid raid : nr.activeRaids().values()) {
            raid.fixBossPosition();
            if (raid.stage() == 2 || raid.stage() == 4) {
                List<PokemonEntity> toRemove = new ArrayList<>();
                for (PokemonEntity pokemonEntity : raid.getClones().keySet()) {
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

    public static void fixPlayerPositions() throws ConcurrentModificationException {
        for (Raid raid : nr.activeRaids().values()) {
            for (ServerPlayerEntity player : nr.server().getPlayerManager().getPlayerList()) {
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
                            if (raid.stage() < 3 && raid.participatingPlayers().contains(player.getUuid()) && raid.stage() != -1 ) {
                                distance = raidRadius - 1;
                            }
                        }

                        if (!Double.isNaN(distance)) {
                            double newX = cx + distance * Math.cos(Math.toRadians(angle));
                            double newZ = cz + distance * Math.sin(Math.toRadians(angle));
                            double newY = raid.raidBossLocation().pos().getY();
                            int chunkX = (int) Math.floor(newX / 16);
                            int chunkZ = (int) Math.floor(newZ / 16);
                            world.setChunkForced(chunkX, chunkZ, true);
                            while (!world.getBlockState(new BlockPos((int) newX, (int) newY, (int) newZ)).isAir()) {
                                newY++;
                            }
                            player.teleport(world, newX, newY, newZ, player.getYaw(), player.getPitch());
                            world.setChunkForced(chunkX, chunkZ, false);
                        }
                    }
                }
            }
        }
    }

    public static void handleDefeatedBosses() throws ConcurrentModificationException {
        List<Raid> toRemove = new ArrayList<>();
        for (Raid raid : nr.activeRaids().values()) {
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

    public static void executeTasks() throws ConcurrentModificationException {
        ServerWorld world = nr.server().getOverworld();
        long currentTick = world.getTime();
        for (Raid raid : nr.activeRaids().values()) {
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

    public static void updateBossbars() throws ConcurrentModificationException {
        for (Raid raid : nr.activeRaids().values()) {
            if (raid.stage() == 2) {
                float progress = (float) raid.currentHealth() / raid.maxHealth();

                if (progress < 0F) {
                    progress = 0F;
                }

                if (progress > 1F) {
                    progress = 1F;
                }

                for (UUID playerUUID : raid.bossbars().keySet()) {
                    try {
                        raid.bossbars().get(playerUUID).progress(progress);
                    } catch (IllegalArgumentException | NullPointerException e) {
                        nr.logError("Error updating bossbar for player uuid: " + playerUUID);
                        nr.logError("Error Message: " + e.getMessage());
                    }
                }
            } else {
                float remainingTicks = (float) (raid.phaseEndTime() - nr.server().getOverworld().getTime());
                float progress = 1.0F / (raid.phaseLength() * 20L);
                float total = progress * remainingTicks;

                if (total < 0F) {
                    total = 0F;
                }

                if (total > 1F) {
                    total = 1F;
                }

                for (UUID playerUUID : raid.bossbars().keySet()) {
                    try {
                        if (raid.bossbars().containsKey(playerUUID) && raid.bossbars().get(playerUUID) != null) {
                            raid.bossbars().get(playerUUID).progress(total);
                        }
                    } catch (IllegalArgumentException | NullPointerException e) {
                        nr.logError("Error updating bossbar for player uuid: " + playerUUID);
                        nr.logError("Error Message: " + e.getMessage());
                    }
                }
            }

            raid.showOverlay(raid.bossbarData());
        }
    }

    public static void fixPlayerPokemon() throws ConcurrentModificationException {
        if (!nr.config().hideOtherPokemonInRaid) {
            for (Raid raid : nr.activeRaids().values()) {
                if (raid.stage() == 2) {
                    for (UUID playerUUID : raid.participatingPlayers()) {
                        ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(playerUUID);
                        if (player != null) {
                            PokemonBattle battle = BattleRegistry.getBattleByParticipatingPlayer(player);
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
                                                    double cx = raid.raidBossLocation().pos().getX();
                                                    double cz = raid.raidBossLocation().pos().getZ();

                                                    // Get direction vector
                                                    double deltaX = x - cx;
                                                    double deltaZ = z - cz;

                                                    // Get angle of approach
                                                    double angle = Math.toDegrees(Math.atan2(deltaZ, deltaX));

                                                    if (angle < 0) {
                                                        angle += 360;
                                                    }

                                                    double distance = Math.min(30, raid.raidBossLocation().borderRadius());

                                                    double newX = cx + distance * Math.cos(Math.toRadians(angle));
                                                    double newZ = cz + distance * Math.sin(Math.toRadians(angle));

                                                    entity.setPosition(newX, raid.raidBossLocation().pos().getY(), newZ);
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

    public static void scheduledRaids() throws ConcurrentModificationException {
        ZonedDateTime now = ZonedDateTime.now(nr.schedulesConfig().zone);
        for (Schedule schedule : nr.schedulesConfig().schedules) {
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
                                    Boss boss = nr.bossesConfig().getRandomBoss(category.id());
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
