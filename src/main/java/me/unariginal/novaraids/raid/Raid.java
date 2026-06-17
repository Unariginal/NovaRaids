package me.unariginal.novaraids.raid;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.drop.DropTable;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty;
import kotlin.Unit;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.cache.PlayerRaidCache;
import me.unariginal.novaraids.config.*;
import me.unariginal.novaraids.data.*;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.data.categories.bosses.CatchPlacement;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.data.categories.modifiers.CategoryModifier;
import me.unariginal.novaraids.data.categories.modifiers.PlaceModifier;
import me.unariginal.novaraids.data.events.WebhookEvent;
import me.unariginal.novaraids.data.players.PlayerRaidData;
import me.unariginal.novaraids.data.rewards.DistributionSection;
import me.unariginal.novaraids.data.rewards.Place;
import me.unariginal.novaraids.data.rewards.RewardDistribution;
import me.unariginal.novaraids.events.RaidEvents;
import me.unariginal.novaraids.handlers.BattleHandler;
import me.unariginal.novaraids.handlers.WebhookHandler;
import me.unariginal.novaraids.utils.ContrabandUtils;
import me.unariginal.novaraids.utils.GlowUtils;
import me.unariginal.novaraids.placeholders.ParseContext;
import net.kyori.adventure.bossbar.BossBar;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static me.unariginal.novaraids.NovaRaids.*;
import static me.unariginal.novaraids.config.ConfigManager.*;
import static me.unariginal.novaraids.utils.TextUtils.deserialize;
import static me.unariginal.novaraids.utils.TextUtils.parse;

public class Raid {
    private final NovaRaids nr = NovaRaids.INSTANCE;

    public final UUID uuid = UUID.randomUUID();
    public final Boss boss;
    public Pokemon bossPokemon;
    public final Pokemon bossPokemonUncatchable = new Pokemon();
    public String baseGimmick;
    public PokemonEntity bossEntity;
    public final String locationId;
    public final LocationsConfig location;
    public final Category category;
    public CategoryModifier modifier = null;
    public final UUID startingPlayer;
    public final ItemStack startingItem;

    public int minPlayers = 0;
    public int maxPlayers = -1;

    public Boolean requiresPass;

    public final List<UUID> participatingPlayers = new ArrayList<>();
    public final List<UUID> markForDeletion = new ArrayList<>();

    public final Map<UUID, Integer> damageByPlayer = new HashMap<>();
    public final List<UUID> latestDamage = new ArrayList<>();

    public final Map<Long, List<Task>> tasks = new HashMap<>();

    public final Map<UUID, BossBar> playerBossbars = new HashMap<>();
    public final List<UUID> fleeingPlayers = new ArrayList<>();
    public final Map<PokemonEntity, UUID> clones = new HashMap<>();
    public final List<EmptyPokeBallEntity> pokeballsCapturing = new ArrayList<>();
    public final Map<String, PlayerRaidData> playerRaidData = new HashMap<>();

    public int currentHealth;
    public int maxHealth;
    public LocalDateTime realStartTime = LocalDateTime.now(SCHEDULES.getTimezone());
    public LocalDateTime realEndTime = LocalDateTime.now(SCHEDULES.getTimezone()).plusSeconds(1);
    public long startTime = nr.server.getOverworld().getTime();
    public long endTime = nr.server.getOverworld().getTime() + 1;
    public long phaseLength;
    public long phaseStartTime;
    public long fightStartTime;
    public long fightEndTime;
    public BossbarsConfig bossbarData;

    public long webhookID = 0;
    public Integer webhookDamage = null;
    public WebhookEvent currentWebhookEvent = null;

    public RaidStatus raidStatus = RaidStatus.IN_PROGRESS;
    public RaidPhase phase = RaidPhase.INIT;

    private final ParseContext.Builder parseContextBuilder = ParseContext.builder().raid(this);

    public Raid(@NotNull Boss boss, @NotNull String locationId, @Nullable ServerPlayerEntity startingPlayer, @Nullable ItemStack startingItem, @Nullable Boolean requiresPass) {
        this.boss = boss;
        parseContextBuilder.boss(boss);
        this.locationId = locationId;
        this.location = LocationsConfig.getLocation(locationId);
        this.startingPlayer = startingPlayer == null ? null : startingPlayer.getUuid();
        this.startingItem = startingItem == null ? null : startingItem.copyWithCount(1);

        category = Category.getCategory(boss.categoryId);
        if (category != null) {
            parseContextBuilder.category(category);
            modifier = CategoryModifier.getRandomModifier(category.categoryId);
            parseContextBuilder.modifier(modifier);
            minPlayers = category.raidDetails.minPlayerCount;
            maxPlayers = category.raidDetails.maxPlayerCount;
            this.requiresPass = requiresPass == null ? category.raidDetails.requirePass : requiresPass;

            setupPhase();
        } else {
            LOGGER.error("[NovaRaids] Category was null, failed to create raid object!");
            phase = RaidPhase.STOPPING;
        }
    }

    public void stop() {
        phase = RaidPhase.STOPPING;

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
        realEndTime = LocalDateTime.now(SCHEDULES.getTimezone());
    }

    public void setupPhase() {
        phase = RaidPhase.SETUP;

        bossPokemon = boss.pokemonDetails.createPokemon(modifier);
        bossPokemonUncatchable.copyFrom(bossPokemon);
        if (boss.pokemonDetails.level > 100) {
            int finalLevel = boss.pokemonDetails.level;
            if (modifier != null) finalLevel += modifier.bossPokemonModifiers.levelOffset;
            if (finalLevel <= 100) bossPokemonUncatchable.setLevel(finalLevel);
            else {
                try {
                    Field levelField = bossPokemonUncatchable.getClass().getDeclaredField("level");
                    levelField.setAccessible(true);
                    levelField.set(bossPokemonUncatchable, finalLevel);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    NovaRaids.LOGGER.error("[NovaRaids] Failed to set pokemon level above 100.", e);
                }
            }
        }
        bossPokemonUncatchable.heal();
        bossPokemonUncatchable.getCustomProperties().add(UncatchableProperty.INSTANCE.uncatchable());
        baseGimmick = boss.pokemonDetails.getRandomGimmick();
        bossEntity = generateBossEntity();
        bossEntity.setBodyYaw(location.bossLocation.yaw);

        if ((modifier != null && modifier.bossDetailModifiers.glowingOverride) || boss.bossDetails.applyGlowing) {
            if (modifier != null && modifier.bossDetailModifiers.glowColorOverrideToggle) {
                GlowUtils.applyGlowing(modifier.bossDetailModifiers.glowColorOverride, bossPokemonUncatchable);
            } else {
                GlowUtils.applyGlowing(boss.bossDetails.glowColor, bossPokemonUncatchable);
            }
        }

        maxHealth = boss.bossDetails.baseHealth;
        if (modifier != null) maxHealth += modifier.bossDetailModifiers.baseHealthOffset;
        currentHealth = maxHealth;

        bossbarData = BossbarsConfig.getBossbar(boss.raidDetails.bossbars.setup);
        showBossbar(bossbarData);

        phaseLength = boss.raidDetails.setupPhaseTime + (modifier == null ? 0 : modifier.raidDetailModifiers.setupPhaseTimeOffset);
        phaseStartTime = nr.server.getOverworld().getTime();

        RaidEvents.SETUP_PHASE_EVENT_PRE.invoker().onSetupPhasePre(this);

        addTask(location.getServerWorld(), phaseLength * 20L, this::fightPhase);

        if (CONFIG.itemSettings.voucherSettings.vouchersJoinRaids) {
            if (startingPlayer != null && startingItem != null) {
                if (addPlayer(startingPlayer, true)) {
                    ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(startingPlayer);
                    if (player != null) player.sendMessage(deserialize(MESSAGES.feedback.joinedRaid, parseContextBuilder.player(player).build()));
                }
            }
        }
    }

    public void fightPhase() {
        RaidEvents.SETUP_PHASE_EVENT_POST.invoker().onSetupPhasePost(this);
        if (participatingPlayers.size() >= minPlayers && !participatingPlayers.isEmpty()) {
            phase = RaidPhase.FIGHT;

            bossbarData = BossbarsConfig.getBossbar(boss.raidDetails.bossbars.fight);
            showBossbar(bossbarData);

            phaseLength = boss.raidDetails.fightPhaseTime + (modifier == null ? 0 : modifier.raidDetailModifiers.fightPhaseTimeOffset);
            phaseStartTime = nr.server.getOverworld().getTime();
            fightStartTime = phaseStartTime;

            RaidEvents.FIGHT_PHASE_EVENT_PRE.invoker().onFightPhasePre(this);

            addTask(location.getServerWorld(), phaseLength * 20L, this::raidLost);
        } else {
            phase = RaidPhase.STOPPING;
            participatingBroadcast(deserialize(MESSAGES.notEnoughPlayers, parseContextBuilder.build()));
            if (requiresPass) {
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
        phase = RaidPhase.STOPPING;
        tasks.clear();
        endTime = nr.server.getOverworld().getTime();
        realEndTime = LocalDateTime.now(SCHEDULES.getTimezone());
        raidStatus = RaidStatus.LOST;
        RaidHistory raidHistory = RaidManager.writeHistory(uuid);
        if (raidHistory != null) {
            ConfigManager.saveRaid(raidHistory);
        } else {
            logError("Failed to save raid history! History was null.");
        }
        RaidEvents.RAID_LOST_EVENT_POST.invoker().onRaidLostPost(this);
    }

    public void preCatchPhase() {
        RaidEvents.FIGHT_PHASE_EVENT_POST.invoker().onFightPhasePost(this);
        phase = RaidPhase.PRE_CATCH;

        boolean doCatchPhase = modifier != null && modifier.raidDetailModifiers.catchPhaseOverrideToggle ? modifier.raidDetailModifiers.doCatchPhaseOverride : boss.raidDetails.doCatchPhase;
        if (doCatchPhase) {
            bossbarData = BossbarsConfig.getBossbar(boss.raidDetails.bossbars.preCatch);
            showBossbar(bossbarData);

            phaseLength = boss.raidDetails.preCatchPhaseTime + (modifier == null ? 0 : modifier.raidDetailModifiers.preCatchPhaseTimeOffset);
        }
        phaseStartTime = nr.server.getOverworld().getTime();
        fightEndTime = phaseStartTime;

        tasks.clear();

        endBattles();

        bossEntity.kill();
        showLeaderboard();
        handleRewards();

        if (doCatchPhase) {
            RaidEvents.CATCH_WARNING_PHASE_EVENT_PRE.invoker().onCatchWarningPhasePre(this);
            addTask(location.getServerWorld(), phaseLength * 20L, this::catchPhase);
        } else {
            raidWon();
        }
    }

    public void catchPhase() {
        RaidEvents.CATCH_WARNING_PHASE_EVENT_POST.invoker().onCatchWarningPhasePost(this);
        phase = RaidPhase.CATCH;

        bossbarData = BossbarsConfig.getBossbar(boss.raidDetails.bossbars.catchPhase);
        showBossbar(bossbarData);

        phaseLength = boss.raidDetails.catchPhaseTime + (modifier == null ? 0 : modifier.raidDetailModifiers.catchPhaseTimeOffset);
        phaseStartTime = nr.server.getOverworld().getTime();

        List<ServerPlayerEntity> alreadyCatching = new ArrayList<>();
        for (CatchPlacement placement : boss.catchSettings.places) {
            List<ServerPlayerEntity> playersToReward = new ArrayList<>();
            if (StringUtils.isNumeric(placement.place)) {
                int placeIndex = Integer.parseInt(placement.place);
                placeIndex--;
                if (placeIndex >= 0 && placeIndex < getDamageLeaderboard().size()) {
                    ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(getDamageLeaderboard().keySet().stream().toList().get(placeIndex));
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
                        ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(getDamageLeaderboard().keySet().stream().toList().get(i));
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
                    float shinyChance = placement.shinyChance;
                    int minPerfectIvs = placement.minPerfectIvs;
                    if (modifier != null) {
                        for (PlaceModifier placeModifier : modifier.catchSettingModifiers.placeModifiers) {
                            if (placeModifier.place.equalsIgnoreCase(placement.place)) {
                                if (placeModifier.overrideShinyChance) shinyChance = placeModifier.shinyChance;
                                else shinyChance += placeModifier.shinyChance;

                                if (placeModifier.overrideMinPerfectIvs) minPerfectIvs = placeModifier.minPerfectIvs;
                                else minPerfectIvs += placeModifier.minPerfectIvs;

                                break;
                            }
                        }
                    }
                    BattleHandler.invokeCatchEncounter(this, player, shinyChance, minPerfectIvs);
                }
            }
        }

        RaidEvents.CATCH_PHASE_EVENT_PRE.invoker().onCatchPhasePre(this);

        addTask(location.getServerWorld(), phaseLength * 20L, this::raidWon);
    }

    public void raidWon() {
        tasks.clear();

        endTime = nr.server.getOverworld().getTime();
        realEndTime = LocalDateTime.now(SCHEDULES.getTimezone());

        boolean doCatchPhase = modifier != null && modifier.raidDetailModifiers.catchPhaseOverrideToggle ? modifier.raidDetailModifiers.doCatchPhaseOverride : boss.raidDetails.doCatchPhase;
        if (doCatchPhase) RaidEvents.CATCH_PHASE_EVENT_POST.invoker().onCatchPhasePost(this);

        raidStatus = RaidStatus.WON;

        RaidHistory raidHistory = RaidManager.writeHistory(uuid);
        if (raidHistory != null) {
            ConfigManager.saveRaid(raidHistory);
        } else {
            logError("Failed to save raid history! History was null.");
        }
        // TODO: This shouldn't be like this
        RaidEvents.RAID_END_EVENT_PRE.invoker().onRaidEndPre(this);
        RaidEvents.RAID_END_EVENT_POST.invoker().onRaidEndPost(this);

        phase = RaidPhase.STOPPING;
    }

    public void showLeaderboard() {
        ParseContext parseContext = parseContextBuilder.build();
        participatingBroadcast(deserialize(MESSAGES.leaderboard.header, parseContext));
        int placeIndex = 0;
        Set<Map.Entry<UUID, Integer>> leaderboardEntries = getDamageLeaderboard().entrySet();
        for (Map.Entry<UUID, Integer> entry : leaderboardEntries) {
            ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                placeIndex++;
                participatingBroadcast(deserialize(parse(MESSAGES.leaderboard.placement, player, entry.getValue(), placeIndex), parseContext));
                if (placeIndex == 10) {
                    break;
                }
            }
        }
        placeIndex = 0;
        for (Map.Entry<UUID, Integer> entry : leaderboardEntries) {
            ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                placeIndex++;
                player.sendMessage(deserialize(parse(MESSAGES.leaderboard.individual, player, entry.getValue(), placeIndex), parseContext));
            }
        }
    }

    public static List<RewardDistribution> getRewardDistribution(Boss boss, Category category, @Nullable CategoryModifier modifier) {
        List<RewardDistribution> categoryRewards = new ArrayList<>();

        // Fill category rewards
        if (modifier != null) {
            categoryRewards.addAll(modifier.raidDetailModifiers.rewardDistribution);
            if (!modifier.raidDetailModifiers.overrideCategoryDistribution) {
                List<Place> overriddenPlacements = new ArrayList<>();
                for (RewardDistribution modifierReward : modifier.raidDetailModifiers.rewardDistribution) {
                    for (Place place : modifierReward.places) {
                        if (place.overrideCategoryReward != null && place.overrideCategoryReward) {
                            overriddenPlacements.add(place);
                        }
                    }
                }

                for (RewardDistribution categoryReward : category.rewardDistribution) {
                    boolean overridden = false;
                    for (Place place : categoryReward.places) {
                        if (overriddenPlacements.stream().anyMatch(overriddenPlace -> place.place.equalsIgnoreCase(overriddenPlace.place))) {
                            overridden = true;
                            break;
                        }
                    }
                    if (!overridden) categoryRewards.add(categoryReward);
                }
            }
        } else {
            categoryRewards.addAll(category.rewardDistribution);
        }

        List<RewardDistribution> rewards = new ArrayList<>(boss.raidDetails.rewardDistribution);

        // Finalize the distribution
        if (!boss.raidDetails.overrideCategoryDistribution) {
            List<Place> overriddenPlacements = new ArrayList<>();
            for (RewardDistribution bossReward : boss.raidDetails.rewardDistribution) {
                for (Place place : bossReward.places) {
                    if (place.overrideCategoryReward != null && place.overrideCategoryReward) {
                        overriddenPlacements.add(place);
                    }
                }
            }

            for (RewardDistribution categoryReward : categoryRewards) {
                boolean overridden = false;
                for (Place place : categoryReward.places) {
                    if (overriddenPlacements.stream().anyMatch(overriddenPlace -> place.place.equalsIgnoreCase(overriddenPlace.place))) {
                        overridden = true;
                        break ;
                    }
                }
                if (!overridden) rewards.add(categoryReward);
            }
        }

        return rewards;
    }

    public void handleRewards() {
        List<RewardDistribution> rewards = getRewardDistribution(boss, category, modifier);
        LinkedHashMap<UUID, Integer> leaderboard = getDamageLeaderboard();

        Map<String, Integer> placementCount = new HashMap<>();
        for (RewardDistribution reward : rewards) {
            for (Place place : reward.places) {
                placementCount.put(
                        place.place.toLowerCase(),
                        placementCount.containsKey(place.place.toLowerCase()) ? placementCount.get(place.place.toLowerCase()) + 1 : 1
                );
            }
        }

        Map<UUID, String> rewardedPlayers = new HashMap<>();
        for (RewardDistribution reward : rewards) {
            for (Place place : reward.places) {
                List<UUID> playersToReward = new ArrayList<>();

                if (StringUtils.isNumeric(place.place)) {
                    int placeAsInt = Integer.parseInt(place.place);
                    placeAsInt--;
                    if (placeAsInt >= 0 && placeAsInt < leaderboard.size()) {
                        UUID playerUuid = leaderboard.keySet().stream().toList().get(placeAsInt);
                        if (!place.requireDamage || (damageByPlayer.containsKey(playerUuid) && damageByPlayer.get(playerUuid) > 0)) {
                            playersToReward.add(playerUuid);
                        }
                    }
                } else if (place.place.contains("%")) {
                    String percentStr = place.place.replace("%", "");
                    if (StringUtils.isNumeric(percentStr)) {
                        int percent = Integer.parseInt(percentStr);
                        double positions = leaderboard.size() * ((double) percent / 100);
                        for (int i = 0; i < ((int) Math.ceil(positions)); i++) {
                            UUID playerUuid = leaderboard.keySet().stream().toList().get(i);
                            if (!place.requireDamage || (damageByPlayer.containsKey(playerUuid) && damageByPlayer.get(playerUuid) > 0)) {
                                playersToReward.add(playerUuid);
                            }
                        }
                    } else logError("Invalid percentage in placement! " + place.place);
                } else if (place.place.equalsIgnoreCase("participating")) {
                    for (UUID playerUuid : participatingPlayers) {
                        if (!place.requireDamage || (damageByPlayer.containsKey(playerUuid) && damageByPlayer.get(playerUuid) > 0)) {
                            playersToReward.add(playerUuid);
                        }
                    }
                }

                for (UUID playerUuid : playersToReward) {
                    ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(playerUuid);
                    if (player == null) continue;

                    boolean duplicatePlacementExists = placementCount.get(place.place.toLowerCase()) > 1;
                    if (!rewardedPlayers.containsKey(playerUuid) || (duplicatePlacementExists && place.place.equalsIgnoreCase(rewardedPlayers.get(playerUuid)))) {
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
                                    logError("RewardPool was null!");
                                    continue;
                                }

                                if (reward.rewards.allowDuplicates || !distributedPools.contains(pool.uuid)) {
                                    List<RewardPresetsConfig.Reward> distributionList = pool.distributeRewards();
                                    distributionList.forEach(distributionItem -> distributionItem.grantReward(player));
                                    distributedPools.add(pool.uuid);
                                } else i--;
                            } else logError("RewardPoolSection was null!");
                        }
                    }
                }

                for (UUID playerUuid : playersToReward) {
                    if (!place.allowOtherRewards && !rewardedPlayers.containsKey(playerUuid)) {
                        rewardedPlayers.put(playerUuid, place.place);
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
        if (phase != RaidPhase.STOPPING && phase != RaidPhase.INIT) {
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

        playerRaidData.get(playerUUID.toString()).leftRaid = true;

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
        if (phase == RaidPhase.SETUP) {
            for (UUID ignored : markForDeletion) {
                maxHealth = Math.max(maxHealth - boss.bossDetails.healthIncreasePerPlayer - (modifier == null ? 0 : modifier.bossDetailModifiers.healthIncreaseOffset), boss.bossDetails.baseHealth + (modifier == null ? 0 : modifier.bossDetailModifiers.baseHealthOffset));
                currentHealth = Math.max(currentHealth - boss.bossDetails.healthIncreasePerPlayer - (modifier == null ? 0 : modifier.bossDetailModifiers.healthIncreaseOffset), maxHealth);
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
            ParseContext parseContext = parseContextBuilder.player(player).build();
            if (PlayerRaidCache.isInRaid(playerUUID)) {
                player.sendMessage(deserialize(MESSAGES.feedback.warnings.alreadyInRaid, parseContext));
                return false;
            }

            if (!Permissions.check(player, "novaraids.override")) {
                if (requiresPass && !usedPass) {
                    player.sendMessage(deserialize(MESSAGES.feedback.warnings.noPass, parseContext));
                    return false;
                }

                if (phase != RaidPhase.SETUP) {
                    player.sendMessage(deserialize(MESSAGES.feedback.warnings.notJoinable, parseContext));
                    return false;
                }

                if (ContrabandUtils.hasContraband(player, this)) {
                    return false;
                }

                int numPokemon = 0;
                for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) {
                    if (pokemon != null) {
                        numPokemon++;
                        if (pokemon.getLevel() < boss.raidDetails.minimumLevel + (modifier == null ? 0 : modifier.raidDetailModifiers.minimumLevelOffset)) {
                            player.sendMessage(deserialize(MESSAGES.feedback.warnings.minimumLevel, parseContext));
                            return false;
                        }
                        if (pokemon.getLevel() > boss.raidDetails.maximumLevel + (modifier == null ? 0 : modifier.raidDetailModifiers.maximumLevelOffset)) {
                            player.sendMessage(deserialize(MESSAGES.feedback.warnings.maximumLevel, parseContext));
                            return false;
                        }
                    }
                }

                if (numPokemon < boss.raidDetails.minimumPartySize + (modifier == null ? 0 : modifier.raidDetailModifiers.minimumPartySizeOffset) || numPokemon == 0) {
                    player.sendMessage(deserialize(MESSAGES.feedback.warnings.notEnoughPokemon, parseContext));
                    return false;
                }

                if (numPokemon > boss.raidDetails.maximumPartySize + (modifier == null ? 0 : modifier.raidDetailModifiers.maximumPartySizeOffset)) {
                    player.sendMessage(deserialize(MESSAGES.feedback.warnings.tooManyPokemon, parseContext));
                    return false;
                }
            } else {
                logInfo("Player has permission override!");
            }

            participatingPlayers.add(playerUUID);
            PlayerRaidCache.add(playerUUID, this);
            if (participatingPlayers.size() > 1) {
                maxHealth += boss.bossDetails.healthIncreasePerPlayer + (modifier == null ? 0 : modifier.bossDetailModifiers.healthIncreaseOffset);
                currentHealth += boss.bossDetails.healthIncreasePerPlayer + (modifier == null ? 0 : modifier.bossDetailModifiers.healthIncreaseOffset);
            }
            playerRaidData.put(playerUUID.toString(), new PlayerRaidData(playerUUID.toString(), player.getNameForScoreboard()));

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

        playerRaidData.get(playerUUID.toString()).totalDamage = damage;
    }

    public LinkedHashMap<UUID, Integer> getDamageLeaderboard() {
        Map<UUID, Integer> latestIndex = new HashMap<>();

        // Store the FIRST time each player appeared
        for (int i = 0; i < latestDamage.size(); i++) {
            latestIndex.putIfAbsent(latestDamage.get(i), i);
        }

        return damageByPlayer.entrySet()
                .stream()
                .sorted((a, b) -> {
                    // Higher damage first
                    int damageCompare = Integer.compare(b.getValue(), a.getValue());

                    if (damageCompare != 0) {
                        return damageCompare;
                    }

                    // Earlier damage reach wins ties
                    int aIndex = latestIndex.getOrDefault(a.getKey(), Integer.MAX_VALUE);
                    int bIndex = latestIndex.getOrDefault(b.getKey(), Integer.MAX_VALUE);

                    return Integer.compare(aIndex, bIndex);
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (x, y) -> x,
                        LinkedHashMap::new
                ));
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
                        player.sendActionBar(deserialize(bossbar.actionbarText, parseContextBuilder.player(player).build()));
                    }
                }
            }
        }
    }
}