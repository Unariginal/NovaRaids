package me.unariginal.novaraids.raid;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.drop.DropTable;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty;
import com.mojang.authlib.GameProfile;
import kotlin.Unit;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.cache.PlayerRaidCache;
import me.unariginal.novaraids.config.*;
import me.unariginal.novaraids.data.*;
import me.unariginal.novaraids.data.bosses.Boss;
import me.unariginal.novaraids.data.bosses.CatchPlacement;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.data.events.WebhookEvent;
import me.unariginal.novaraids.data.rewards.DistributionSection;
import me.unariginal.novaraids.data.rewards.Place;
import me.unariginal.novaraids.data.rewards.RewardDistribution;
import me.unariginal.novaraids.events.RaidEvents;
import me.unariginal.novaraids.handlers.BattleHandler;
import me.unariginal.novaraids.handlers.WebhookHandler;
import me.unariginal.novaraids.utils.BanHandler;
import me.unariginal.novaraids.utils.TextUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.UserCache;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static me.unariginal.novaraids.NovaRaids.logError;
import static me.unariginal.novaraids.NovaRaids.logInfo;
import static me.unariginal.novaraids.config.ConfigManager.CONFIG;
import static me.unariginal.novaraids.config.ConfigManager.MESSAGES;

public class Raid {
    private final NovaRaids nr = NovaRaids.INSTANCE;

    public final UUID uuid = UUID.randomUUID();
    public final Boss boss;
    public final Pokemon bossPokemon;
    public final Pokemon bossPokemonUncatchable = new Pokemon();
    public final String baseGimmick;
    public final PokemonEntity bossEntity;
    public final String locationId;
    public final LocationsConfig location;
    public final Category category;
    public final UUID startingPlayer;
    public final ItemStack startingItem;
    public final int minPlayers;
    public final int maxPlayers;
    public final List<UUID> participatingPlayers = new ArrayList<>();
    public final List<UUID> markForDeletion = new ArrayList<>();
    public final Map<UUID, Integer> damageByPlayer = new HashMap<>();
    public final List<UUID> latestDamage = new ArrayList<>();
    public final List<UUID> fleeingPlayers = new ArrayList<>();
    public final Map<Long, List<Task>> tasks = new HashMap<>();
    public final Map<UUID, BossBar> playerBossbars = new HashMap<>();
    public final Map<PokemonEntity, UUID> clones = new HashMap<>();
    public final List<EmptyPokeBallEntity> pokeballsCapturing = new ArrayList<>();
    public int currentHealth;
    public int maxHealth;
    public long startTime = 0;
    public long endTime = 0;
    public long phaseLength;
    public long phaseStartTime;
    public long fightStartTime;
    public long fightEndTime;
    public BossbarsConfig bossbarData;

    public long webhookID = 0;
    public WebhookEvent currentWebhookEvent = null;

    public int stage;

    public Raid(Boss boss, String locationId, ServerPlayerEntity startingPlayer, ItemStack startingItem) {
        this.boss = boss;
        this.locationId = locationId;
        this.location = LocationsConfig.getLocation(locationId);
        this.startingPlayer = startingPlayer == null ? null : startingPlayer.getUuid();
        this.startingItem = startingItem;
        if (startingItem != null) startingItem.setCount(1);

        bossPokemon = boss.pokemonDetails.createPokemon();
        bossPokemonUncatchable.copyFrom(bossPokemon);
        bossPokemonUncatchable.getCustomProperties().add(UncatchableProperty.INSTANCE.uncatchable());
        baseGimmick = boss.pokemonDetails.getRandomGimmick();
        bossEntity = generateBossEntity();
        bossEntity.setBodyYaw(location.bossLocation.yaw);

        maxHealth = boss.bossDetails.baseHealth;
        currentHealth = maxHealth;

        category = Category.getCategory(boss.categoryId);
        minPlayers = category.raidDetails.minPlayerCount;
        maxPlayers = category.raidDetails.maxPlayerCount;

        stage = 0;
        startTime = nr.server.getOverworld().getTime();
        setupPhase();
    }

    public void stop() {
        stage = -1;

        if (bossEntity != null && bossEntity.isAlive() && !bossEntity.isRemoved()) {
            bossEntity.kill();
        }

        endBattles();

        List<EmptyPokeBallEntity> pokeballs = new ArrayList<>(pokeballsCapturing);
        for (EmptyPokeBallEntity entity : pokeballs) {
            if (entity != null) {
                if (entity.isAlive() && !entity.isRemoved()) {
                    entity.remove(Entity.RemovalReason.DISCARDED);
                    PokemonEntity pokemonEntity = entity.getCapturingPokemon();
                    if (pokemonEntity != null) {
                        pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
                    }
                    removePokeballsCapturing(entity);
                }
            }
        }

        List<PokemonEntity> toRemove = new ArrayList<>(clones.keySet());
        for (PokemonEntity pokemon : toRemove) {
            removeClone(pokemon, false);
        }

        for (UUID playerUUID : playerBossbars.keySet()) {
            ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(playerUUID);
            if (player != null) {
                player.hideBossBar(playerBossbars.get(playerUUID));
            }
        }

        PlayerRaidCache.clearFromRaid(uuid);

        endTime = nr.server.getOverworld().getTime();
//        nr.initNextRaid();
    }

    public void setupPhase() {
        stage = 1;

        bossbarData = BossbarsConfig.getBossbar(boss.raidDetails.bossbars.setup);
        showBossbar(bossbarData);

        phaseLength = boss.raidDetails.setupPhaseTime;
        phaseStartTime = nr.server.getOverworld().getTime();

        RaidEvents.SETUP_PHASE_EVENT_PRE.invoker().onSetupPhasePre(this);

        addTask(location.getServerWorld(), phaseLength * 20L, this::fightPhase);

        if (CONFIG.itemSettings.voucherSettings.vouchersJoinRaids) {
            if (startingPlayer != null && startingItem != null) {
                if (addPlayer(startingPlayer, true)) {
                    ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(startingPlayer);
                    if (player != null) player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.joinedRaid, this)));
                }
            }
        }
    }

    public void fightPhase() {
        RaidEvents.SETUP_PHASE_EVENT_POST.invoker().onSetupPhasePost(this);
        if (participatingPlayers.size() >= minPlayers && !participatingPlayers.isEmpty()) {
            stage = 2;

            bossbarData = BossbarsConfig.getBossbar(boss.raidDetails.bossbars.fight);
            showBossbar(bossbarData);

            phaseLength = boss.raidDetails.fightPhaseTime;
            phaseStartTime = nr.server.getOverworld().getTime();
            fightStartTime = phaseStartTime;

            RaidEvents.FIGHT_PHASE_EVENT_PRE.invoker().onFightPhasePre(this);

            addTask(location.getServerWorld(), phaseLength * 20L, this::raidLost);
        } else {
            stage = -1;
            participatingBroadcast(TextUtils.deserialize(TextUtils.parse(MESSAGES.notEnoughPlayers, this)));
            if (category.raidDetails.requirePass) {
                if (startingItem != null) {
                    ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(startingPlayer);
                    if (player != null) {
                        player.giveItemStack(startingItem);
                    }
                }
            }
            if (CONFIG.discordWebhook.enabled && webhookID != 0 && CONFIG.discordWebhook.deleteIfNoFightPhase) {
                WebhookHandler.deleteWebhook(this);
            }
        }
    }

    public void raidLost() {
        RaidEvents.RAID_LOST_EVENT_PRE.invoker().onRaidLostPre(this);
        stage = -1;
        tasks.clear();
        endTime = nr.server.getOverworld().getTime();
        RaidEvents.RAID_LOST_EVENT_POST.invoker().onRaidLostPost(this);
    }

    public void preCatchPhase() {
        RaidEvents.FIGHT_PHASE_EVENT_POST.invoker().onFightPhasePost(this);
        stage = 3;

        if (boss.raidDetails.doCatchPhase) {
            bossbarData = BossbarsConfig.getBossbar(boss.raidDetails.bossbars.preCatch);
            showBossbar(bossbarData);

            phaseLength = boss.raidDetails.preCatchPhaseTime;
        }
        phaseStartTime = nr.server.getOverworld().getTime();
        fightEndTime = phaseStartTime;

        tasks.clear();

        endBattles();

        bossEntity.kill();
        handleRewards();

        // TODO: Raid history (probably also run it after catch phase if it exists)
//        try {
//            CONFIG.writeResults(this);
//        } catch (IOException | NoSuchElementException e) {
//            nr.logError("Failed to write raid information to history file.");
//        }

        if (boss.raidDetails.doCatchPhase) {
            RaidEvents.CATCH_WARNING_PHASE_EVENT_PRE.invoker().onCatchWarningPhasePre(this);
            addTask(location.getServerWorld(), phaseLength * 20L, this::catchPhase);
        } else {
            raidWon();
        }
    }

    public void catchPhase() {
        RaidEvents.CATCH_WARNING_PHASE_EVENT_POST.invoker().onCatchWarningPhasePost(this);
        stage = 4;

        bossbarData = BossbarsConfig.getBossbar(boss.raidDetails.bossbars.catchPhase);
        showBossbar(bossbarData);

        phaseLength = boss.raidDetails.catchPhaseTime;
        phaseStartTime = nr.server.getOverworld().getTime();

        List<ServerPlayerEntity> alreadyCatching = new ArrayList<>();
        for (CatchPlacement placement : boss.catchSettings.places) {
            List<ServerPlayerEntity> playersToReward = new ArrayList<>();
            if (StringUtils.isNumeric(placement.place)) {
                int placeIndex = Integer.parseInt(placement.place);
                placeIndex--;
                if (placeIndex >= 0 && placeIndex < getDamageLeaderboard().size()) {
                    ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(getDamageLeaderboard().get(placeIndex).getKey());
                    if (player != null) {
                        if (!alreadyCatching.contains(player)) {
                            if (!placement.requireDamage || (damageByPlayer.containsKey(player.getUuid()) && damageByPlayer.get(player.getUuid()) > 0)) {
                                playersToReward.add(player);
                            }
                        }
                    }
                }
            } else if (placement.place.contains("%")) {
                String percentStr = placement.place.replace("%", "");
                if (StringUtils.isNumeric(percentStr)) {
                    int percent = Integer.parseInt(percentStr);
                    double positions = getDamageLeaderboard().size() * ((double) percent / 100);
                    for (int i = 0; i < ((int) positions); i++) {
                        ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(getDamageLeaderboard().get(i).getKey());
                        if (player != null) {
                            if (!alreadyCatching.contains(player)) {
                                if (!placement.requireDamage || (damageByPlayer.containsKey(player.getUuid()) && damageByPlayer.get(player.getUuid()) > 0)) {
                                    playersToReward.add(player);
                                }
                            }
                        }
                    }
                }
            } else if (placement.place.equalsIgnoreCase("participating")) {
                for (UUID participatingUUID : participatingPlayers) {
                    ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(participatingUUID);
                    if (player != null) {
                        if (!alreadyCatching.contains(player)) {
                            boolean valid = false;
                            if (placement.requireDamage) {
                                if (damageByPlayer.containsKey(player.getUuid()) && damageByPlayer.get(player.getUuid()) > 0) {
                                    valid = true;
                                }
                            } else valid = true;

                            if (valid) playersToReward.add(player);
                        }
                    }
                }
            }

            for (ServerPlayerEntity player : playersToReward) {
                if (player != null) {
                    alreadyCatching.add(player);
                    BattleHandler.invokeCatchEncounter(this, player, (float) placement.shinyChance, placement.minPerfectIvs);
                }
            }
        }

        RaidEvents.CATCH_PHASE_EVENT_PRE.invoker().onCatchPhasePre(this);

        addTask(location.getServerWorld(), phaseLength * 20L, this::raidWon);
    }

    public void raidWon() {
        stage = -1;
        tasks.clear();

        endTime = nr.server.getOverworld().getTime();

        if (boss.raidDetails.doCatchPhase) {
            RaidEvents.CATCH_PHASE_EVENT_POST.invoker().onCatchPhasePost(this);
        }
        RaidEvents.RAID_END_EVENT_PRE.invoker().onRaidEndPre(this);
        RaidEvents.RAID_END_EVENT_POST.invoker().onRaidEndPost(this);
    }

    public void handleRewards() {
        participatingBroadcast(TextUtils.deserialize(TextUtils.parse(MESSAGES.leaderboard.header, this)));
        int placeIndex = 0;
        for (Map.Entry<String, Integer> entry : getDamageLeaderboard()) {
            ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                placeIndex++;
                participatingBroadcast(TextUtils.deserialize(TextUtils.parse(MESSAGES.leaderboard.placement, this, player, entry.getValue(), placeIndex)));
                if (placeIndex == 10) {
                    break;
                }
            }
        }
        placeIndex = 0;
        for (Map.Entry<String, Integer> entry : getDamageLeaderboard()) {
            ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                placeIndex++;
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.leaderboard.individual, this, player, entry.getValue(), placeIndex)));
            }
        }

        List<RewardDistribution> categoryRewards = new ArrayList<>(category.rewardDistribution);
        List<RewardDistribution> bossRewards = new ArrayList<>(boss.raidDetails.rewardDistribution);

        List<RewardDistribution> rewards = new ArrayList<>(bossRewards);

        if (!boss.raidDetails.overrideCategoryDistribution) {
            List<Place> overriddenPlacements = new ArrayList<>();

            for (RewardDistribution bossReward : bossRewards) {
                List<Place> places = bossReward.places;
                for (Place place : places) {
                    if (place.overrideCategoryReward != null && place.overrideCategoryReward) {
                        overriddenPlacements.add(place);
                    }
                }
            }

            for (RewardDistribution categoryReward : categoryRewards) {
                boolean overridden = false;
                List<Place> places = categoryReward.places;
                outer:
                for (Place place : places) {
                    for (Place overriddenPlacement : overriddenPlacements) {
                        if (overriddenPlacement.place.equalsIgnoreCase(place.place)) {
                            overridden = true;
                            break outer;
                        }
                    }
                }
                if (!overridden) {
                    rewards.add(categoryReward);
                }
            }
        }

        Map<ServerPlayerEntity, String> noMoreRewards = new HashMap<>();
        for (RewardDistribution reward : rewards) {
            List<Place> places = reward.places;
            for (Place place : places) {
                List<ServerPlayerEntity> playersToReward = new ArrayList<>();
                if (StringUtils.isNumeric(place.place)) {
                    int placeAsInt = Integer.parseInt(place.place);
                    placeAsInt--;
                    if (placeAsInt >= 0 && placeAsInt < getDamageLeaderboard().size()) {
                        ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(getDamageLeaderboard().get(placeAsInt).getKey());
                        if (player != null) {
                            if (damageByPlayer.containsKey(player.getUuid())) {
                                if (!place.requireDamage || damageByPlayer.get(player.getUuid()) > 0) {
                                    playersToReward.add(player);
                                }
                            }
                        }
                    }
                } else if (place.place.contains("%")) {
                    String percentStr = place.place.replace("%", "");
                    if (StringUtils.isNumeric(percentStr)) {
                        int percent = Integer.parseInt(percentStr);
                        double positions = getDamageLeaderboard().size() * ((double) percent / 100);
                        for (int i = 0; i < ((int) Math.ceil(positions)); i++) {
                            ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(getDamageLeaderboard().get(i).getKey());
                            if (player != null) {
                                if (damageByPlayer.containsKey(player.getUuid())) {
                                    if (!place.requireDamage || damageByPlayer.get(player.getUuid()) > 0) {
                                        playersToReward.add(player);
                                    }
                                }
                            }
                        }
                    }
                } else if (place.place.equalsIgnoreCase("participating")) {
                    for (UUID participatingUUID : participatingPlayers) {
                        ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(participatingUUID);
                        if (player != null) {
                            boolean valid = false;
                            if (place.requireDamage) {
                                if (damageByPlayer.containsKey(player.getUuid()) && damageByPlayer.get(player.getUuid()) > 0) {
                                    valid = true;
                                }
                            } else valid = true;

                            if (valid) playersToReward.add(player);
                        }
                    }
                }

                for (ServerPlayerEntity player : playersToReward) {
                    if (player != null) {
                        boolean duplicatePlacementExists = false;
                        int placeCount = 0;
                        for (RewardDistribution rewardSection : rewards) {
                            List<Place> rewardPlaces = rewardSection.places;
                            for (Place rewardPlace : rewardPlaces) {
                                if (rewardPlace.place.equalsIgnoreCase(place.place)) {
                                    placeCount++;
                                    break;
                                }
                            }
                            if (placeCount >= 2) {
                                duplicatePlacementExists = true;
                                break;
                            }
                        }

                        if (!noMoreRewards.containsKey(player) || (duplicatePlacementExists && place.place.equalsIgnoreCase(noMoreRewards.get(player)))) {
                            int rolls = new Random().nextInt(reward.rewards.minRolls, reward.rewards.maxRolls + 1);
                            List<UUID> distributedPools = new ArrayList<>();
                            for (int i = 0; i < rolls; i++) {
                                DistributionSection.RewardPoolSection poolSection = reward.rewards.getRandomRewardPool();
                                if (poolSection != null) {
                                    RewardPoolsConfig.RewardPool pool = null;
                                    if (poolSection instanceof DistributionSection.PredefinedRewardPoolSection predefinedPoolSection) {
                                        pool = RewardPoolsConfig.getRewardPool(predefinedPoolSection.poolPreset);
                                    } else if (poolSection instanceof DistributionSection.UndefinedRewardPoolSection undefinedPoolSection) {
                                        pool = undefinedPoolSection.pool;
                                    }
                                    if (pool == null) {
                                        logError("Pool was null!");
                                        continue;
                                    }
                                    if (reward.rewards.allowDuplicates || !distributedPools.contains(pool.uuid)) {
                                        List<RewardPresetsConfig.Reward> distributionList = pool.distributeRewards();
                                        distributionList.forEach(distributionItem -> distributionItem.grantReward(player));
                                        distributedPools.add(pool.uuid);
                                    } else {
                                        i--;
                                    }
                                } else {
                                    logError("Pool was null!");
                                }
                            }
                        }
                    }
                }

                for (ServerPlayerEntity player : playersToReward) {
                    if (!place.allowOtherRewards && !noMoreRewards.containsKey(player)) {
                        noMoreRewards.put(player, place.place);
                    }
                }
            }
        }
    }

    public void addTask(ServerWorld world, Long delay, Runnable action) {
        long currentTick = nr.server.getOverworld().getTime();
        long executeTick = currentTick + delay;

        Task task = new Task(world, executeTick, action);
        if (tasks.containsKey(executeTick)) {
            List<Task> taskList = new ArrayList<>(tasks.get(executeTick));
            taskList.add(task);
            tasks.put(executeTick, taskList);
        } else {
            tasks.put(executeTick, List.of(task));
        }
    }

    public void removeTask(long executeTick) {
        tasks.remove(executeTick);
    }

    public void fixBossPosition() {
        if (stage != -1 && stage != 0) {
            if (bossEntity != null) {
                if (bossEntity.getPos() != location.bossLocation.getPos()) {
                    bossEntity.teleport(location.getServerWorld(), location.bossLocation.xPos, location.bossLocation.yPos, location.bossLocation.zPos, null, location.bossLocation.yaw, 0);
                }
            }
        }
    }

    private PokemonEntity generateBossEntity() {
        ServerWorld world = location.getServerWorld();
        Vec3d pos = location.bossLocation.getPos();
        return bossPokemonUncatchable.sendOut(world, pos, null, entity -> {
            entity.setPersistent();
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 999999, 9999, true, false));
            entity.setMovementSpeed(0.0f);
            entity.setNoGravity(true);
            entity.setAiDisabled(true);
            if (boss.bossDetails.applyGlowing) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 999999, 9999, true, false));
            }
            entity.setInvulnerable(true);
            entity.setBodyYaw(location.bossLocation.yaw);
            entity.setDrops(new DropTable());
            Box hitbox = entity.getBoundingBox();
            hitbox.stretch(new Vec3d(bossPokemonUncatchable.getScaleModifier(), bossPokemonUncatchable.getScaleModifier(), bossPokemonUncatchable.getScaleModifier()));
            entity.setBoundingBox(hitbox);
            return Unit.INSTANCE;
        });
    }

    private void endBattles() {
        for (UUID playerUUID : participatingPlayers) {
            ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(playerUUID);
            if (player != null) {
                PokemonBattle battle = BattleRegistry.getBattleByParticipatingPlayer(player);
                if (battle != null) {
                    battle.stop();
                }
            }
        }
    }

    public String getPhase() {
        return switch (stage) {
            case -1 -> "Stopping";
            case 0 -> "Constructor";
            case 1 -> "Setup";
            case 2 -> "Fight";
            case 3 -> "Pre-Catch";
            case 4 -> "Catch";
            default -> "Error";
        };
    }

    public long raidCompletionTime() {
        if (endTime > 0) {
            return endTime - startTime;
        }
        return 0;
    }

    public long raidTimer() {
        return nr.server.getOverworld().getTime() - startTime;
    }

    public long bossDefeatTime() {
        return fightEndTime - fightStartTime;
    }

    public long phaseEndTime() {
        return phaseStartTime + (phaseLength * 20L);
    }

    public void applyDamage(int damage) {
        currentHealth -= damage;
    }

    public void broadcast(Text text) {
        nr.server.getPlayerManager().getPlayerList().forEach(p -> p.sendMessage(text));
    }

    public void participatingBroadcast(Text text) {
        for (UUID playerUuid : participatingPlayers) {
            ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(playerUuid);
            if (player != null) {
                player.sendMessage(text);
            }
        }
    }

    public void addClone(PokemonEntity pokemon, ServerPlayerEntity player) {
        for (PokemonEntity clone : clones.keySet()) {
            if (clone.getUuid().equals(pokemon.getUuid())) {
                return;
            }
        }
        clones.put(pokemon, player.getUuid());
    }

    public void removeClone(PokemonEntity clone, boolean fromFlee) {
        if (clone != null) {
            if (clone.isAlive()) {
                int chunkX = (int) Math.floor(clone.getPos().getX() / 16);
                int chunkZ = (int) Math.floor(clone.getPos().getZ() / 16);
                ServerWorld world = nr.server.getOverworld();
                for (ServerWorld worldLoop : nr.server.getWorlds()) {
                    if (worldLoop.getRegistryKey().equals(clone.getWorld().getRegistryKey())) {
                        world = worldLoop;
                    }
                }

                ServerWorld finalWorld = world;
                nr.server.execute(() -> {
                    finalWorld.setChunkForced(chunkX, chunkZ, true);
                    if (!fromFlee) {
                        if (clone.isBattling() && clone.getBattleId() != null) {
                            PokemonBattle battle = BattleRegistry.getBattle(clone.getBattleId());
                            if (battle != null) {
                                battle.stop();
                            }
                        }
                    }

                    clone.kill();
                    finalWorld.setChunkForced(chunkX, chunkZ, false);
                });
            }
        }
        clones.remove(clone);
    }

    public boolean isParticipating(ServerPlayerEntity player) {
        return isParticipating(player.getUuid());
    }

    public boolean isParticipating(UUID playerUUID) {
        return participatingPlayers.contains(playerUUID);
    }

    public void removePlayer(ServerPlayerEntity player) {
        removePlayer(player.getUuid());
    }

    public void removePlayer(UUID playerUUID) {
        if (!isParticipating(playerUUID)) {
            return;
        }

        ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(playerUUID);
        if (player != null) {
            markForDeletion.add(playerUUID);
            player.hideBossBar(playerBossbars.get(playerUUID));
            playerBossbars.remove(playerUUID);

            List<PokemonEntity> toRemove = new ArrayList<>();
            for (PokemonEntity clone : clones.keySet()) {
                if (clones.get(clone).equals(player.getUuid())) {
                    toRemove.add(clone);
                }
            }
            for (PokemonEntity clone : toRemove) {
                removeClone(clone, false);
            }
        }
    }

    public void removePlayers() {
        if (stage == 1) {
            for (UUID ignored : markForDeletion) {
                maxHealth = Math.max(maxHealth - boss.bossDetails.healthIncreasePerPlayer, boss.bossDetails.baseHealth);
                currentHealth = Math.max(currentHealth - boss.bossDetails.healthIncreasePerPlayer, boss.bossDetails.baseHealth);
            }
        }

        for (UUID uuid : markForDeletion) {
            PlayerRaidCache.remove(uuid);
            participatingPlayers.removeIf(uuid::equals);
        }

        markForDeletion.clear();
    }

    public boolean addPlayer(UUID playerUUID, boolean usedPass) {
        ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(playerUUID);
        if (player != null) {
            if (PlayerRaidCache.isInRaid(playerUUID)) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.alreadyInRaid, this)));
                return false;
            }

            if (!Permissions.check(player, "novaraids.override")) {
                if (category.raidDetails.requirePass && !usedPass) {
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.noPass, this)));
                    return false;
                }

                if (stage != 1) {
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.notJoinable, this)));
                    return false;
                }

                if (BanHandler.hasContraband(player, boss)) {
                    return false;
                }

                int numPokemon = 0;
                for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) {
                    if (pokemon != null) {
                        numPokemon++;
                        if (pokemon.getLevel() < boss.raidDetails.minimumLevel) {
                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.minimumLevel, this)));
                            return false;
                        }
                        if (pokemon.getLevel() > boss.raidDetails.maximumLevel) {
                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.maximumLevel, this)));
                            return false;
                        }
                    }
                }

                // TODO: Configurable min + max party size
                if (numPokemon == 0) {
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.notEnoughPokemon, this)));
                    return false;
                }
            } else {
                logInfo("Player has permission override!");
            }

            participatingPlayers.add(playerUUID);
            PlayerRaidCache.add(playerUUID, this);
            if (participatingPlayers.size() > 1) {
                maxHealth += boss.bossDetails.healthIncreasePerPlayer;
                currentHealth += boss.bossDetails.healthIncreasePerPlayer;
            }

            showBossbar(bossbarData, player);
            if (location.useJoinLocation) {
                player.teleport(location.getServerWorld(), location.joinLocation.xPos, location.joinLocation.yPos, location.joinLocation.zPos, location.joinLocation.yaw, location.joinLocation.pitch);
            }
            return true;
        }
        return false;
    }

    public void updatePlayerDamage(UUID playerUUID, int damage) {
        if (damageByPlayer.containsKey(playerUUID)) {
            damage += damageByPlayer.get(playerUUID);
        }
        damageByPlayer.put(playerUUID, damage);
        latestDamage.remove(playerUUID);
        latestDamage.add(playerUUID);
    }

    public List<Map.Entry<String, Integer>> getDamageLeaderboard() {
        List<Map.Entry<UUID, Integer>> leaderboardList = new ArrayList<>(damageByPlayer.entrySet());

        Map<Integer, Long> damageFrequencies = leaderboardList.stream().collect(
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)).values().stream().collect(
                Collectors.groupingBy(value -> value, Collectors.counting()));

        List<Integer> duplicates = damageFrequencies.entrySet().stream().filter(
                entry -> entry.getValue() > 1).map(Map.Entry::getKey).toList();

        leaderboardList.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        for (int n = leaderboardList.size(); n > 0; n--) {
            if (n == 1) {
                break;
            }
            for (int i = 0; i < n - 1; i++) {
                Map.Entry<UUID, Integer> e1 = leaderboardList.get(i);
                Map.Entry<UUID, Integer> e2 = leaderboardList.get(i + 1);
                boolean duplicate = false;
                for (int value : duplicates) {
                    if (e1.getValue() == value) {
                        duplicate = true;
                        break;
                    }
                    if (e2.getValue() == value) {
                        duplicate = true;
                        break;
                    }
                }
                if (duplicate && e1.getValue().compareTo(e2.getValue()) == 0) {
                    for (UUID uDmg : latestDamage) {
                        if (e1.getKey().equals(uDmg)) {
                            break;
                        }
                        if (e2.getKey().equals(uDmg)) {
                            Map.Entry<UUID, Integer> temp = leaderboardList.get(i);
                            leaderboardList.set(i, leaderboardList.get(i + 1));
                            leaderboardList.set(i + 1, temp);
                            break;
                        }
                    }
                }
            }
        }

        List<Map.Entry<String, Integer>> sortedLeaderboard = new ArrayList<>();
        UserCache cache = nr.server.getUserCache();
        if (cache != null) {
            for (Map.Entry<UUID, Integer> entry : leaderboardList) {
                Optional<GameProfile> profile = cache.getByUuid(entry.getKey());
                if (profile.isPresent()) {
                    String name = profile.get().getName();
                    sortedLeaderboard.add(Map.entry(name, entry.getValue()));
                }
            }
        }

        return sortedLeaderboard;
    }

    public void addPokeballsCapturing(EmptyPokeBallEntity entity) {
        pokeballsCapturing.add(entity);
    }

    public void removePokeballsCapturing(EmptyPokeBallEntity entity) {
        pokeballsCapturing.remove(entity);
    }

    public boolean isPlayerFleeing(ServerPlayerEntity player) {
        return isPlayerFleeing(player.getUuid());
    }

    public boolean isPlayerFleeing(UUID playerUUID) {
        return fleeingPlayers.contains(playerUUID);
    }

    public void addFleeingPlayer(UUID playerUUID) {
        fleeingPlayers.add(playerUUID);
    }

    private void showBossbar(BossbarsConfig bossbar, ServerPlayerEntity player) {
        BossBar bar = bossbar.createBossbar(this);
        player.showBossBar(bar);
        playerBossbars.put(player.getUuid(), bar);
    }

    private void showBossbar(BossbarsConfig bossbar) {
        hideBossbar();
        if (bossbar != null) {
            Collection<UUID> playerCache = new ArrayList<>(participatingPlayers);
            for (UUID playerUUID : playerCache) {
                ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(playerUUID);
                if (player != null) {
                    BossBar bar = bossbar.createBossbar(this);
                    player.showBossBar(bar);
                    playerBossbars.put(playerUUID, bar);
                }
            }
        }
    }

    private void hideBossbar() {
        for (UUID playerUUID : playerBossbars.keySet()) {
            ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(playerUUID);
            if (player != null) {
                player.hideBossBar(playerBossbars.get(playerUUID));
            }
        }
        playerBossbars.clear();
    }

    public void showOverlay(BossbarsConfig bossbar) {
        if (bossbar != null) {
            if (bossbar.useActionbar) {
                Collection<UUID> playerCache = new ArrayList<>(playerBossbars.keySet());
                for (UUID playerUUID : playerCache) {
                    ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(playerUUID);
                    if (player != null) {
                        player.sendActionBar(TextUtils.deserialize(TextUtils.parse(bossbar.actionbarText, this)));
                    }
                }
            }
        }
    }
}