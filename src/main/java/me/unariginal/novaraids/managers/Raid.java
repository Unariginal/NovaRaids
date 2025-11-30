package me.unariginal.novaraids.managers;

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
import me.unariginal.novaraids.config.MessagesConfig;
import me.unariginal.novaraids.data.*;
import me.unariginal.novaraids.data.bosssettings.Boss;
import me.unariginal.novaraids.data.bosssettings.CatchPlacement;
import me.unariginal.novaraids.data.rewards.DistributionSection;
import me.unariginal.novaraids.data.rewards.Place;
import me.unariginal.novaraids.data.rewards.RewardPool;
import me.unariginal.novaraids.utils.BanHandler;
import me.unariginal.novaraids.utils.RandomUtils;
import me.unariginal.novaraids.utils.TextUtils;
import me.unariginal.novaraids.utils.WebhookHandler;
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

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class Raid {
    private final NovaRaids nr = NovaRaids.INSTANCE;
    private final MessagesConfig messages = nr.messagesConfig();

    private final UUID uuid;
    private final Boss bossInfo;
    private final Pokemon raidBossPokemon;
    private final Pokemon raidBossPokemonUncatchable;
    private final String baseGimmick;
    private final PokemonEntity raidBossEntity;
    private final Location raidBossLocation;
    private final Category raidBossCategory;

    private int currentHealth;
    private int maxHealth;

    private final UUID startedBy;
    private final ItemStack startingItem;

    private final int minPlayers;
    private final int maxPlayers;
    private final List<UUID> participatingPlayers = new ArrayList<>();
    private final List<UUID> markForDeletion = new ArrayList<>();
    private boolean clearToDelete = true;
    private final Map<UUID, Integer> damageByPlayer = new HashMap<>();
    private final List<UUID> latestDamage = new ArrayList<>();
    private final List<UUID> fleeingPlayers = new ArrayList<>();

    private final Map<Long, List<Task>> tasks = new HashMap<>();
    private final Map<UUID, BossBar> playerBossbars = new HashMap<>();

    private final Map<PokemonEntity, UUID> clones = new HashMap<>();
    private final List<EmptyPokeBallEntity> pokeballsCapturing = new ArrayList<>();

    private long raidStartTime = 0;
    private long raidEndTime = 0;
    private long phaseLength;
    private long phaseStartTime;
    private long fightStartTime;
    private long fightEndTime;
    private BossbarData bossbarData;

    private long webhook = 0;

    private int stage;

    public Raid(Boss bossInfo, Location raidBossLocation, UUID startedBy, ItemStack startingItem) {
        this.bossInfo = bossInfo;
        this.raidBossLocation = raidBossLocation;
        this.startedBy = startedBy;
        this.startingItem = startingItem;
        if (startingItem != null) {
            startingItem.setCount(1);
        }

        raidBossPokemon = bossInfo.pokemonDetails().createPokemon(false);
        raidBossPokemonUncatchable = bossInfo.pokemonDetails().createPokemon(false);
        raidBossPokemonUncatchable.getCustomProperties().add(UncatchableProperty.INSTANCE.uncatchable());
        baseGimmick = bossInfo.pokemonDetails().getGimmick();
        raidBossEntity = generateBossEntity();
        raidBossEntity.setBodyYaw(raidBossLocation.bossFacingDirection());
        uuid = raidBossEntity.getUuid();

        maxHealth = bossInfo.baseHealth();
        currentHealth = maxHealth;

        raidBossCategory = nr.bossesConfig().getCategory(bossInfo.categoryId());
        minPlayers = raidBossCategory.minPlayers();
        maxPlayers = raidBossCategory.maxPlayers();

        stage = 0;
        raidStartTime = nr.server().getOverworld().getTime();
        setupPhase();
    }

    public void stop() {
        stage = -1;

        if (raidBossEntity != null && raidBossEntity.isAlive() && !raidBossEntity.isRemoved()) {
            raidBossEntity.kill();
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
            ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(playerUUID);
            if (player != null) {
                player.hideBossBar(playerBossbars.get(playerUUID));
            }
        }

        raidEndTime = nr.server().getOverworld().getTime();
        nr.initNextRaid();
    }

    public void setupPhase() {
        stage = 1;

        bossbarData = nr.bossbarsConfig().getBossbar(bossInfo, "setup");
        showBossbar(bossbarData);

        phaseLength = bossInfo.raidDetails().setupPhaseTime();
        phaseStartTime = nr.server().getOverworld().getTime();

        broadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("start_pre_phase"), this)));
        nr.messagesConfig().executeCommand(this);

        if (WebhookHandler.webhookToggle &&
                WebhookHandler.startEmbedEnabled &&
                !WebhookHandler.blacklistedBosses.contains(bossInfo.bossId()) &&
                !WebhookHandler.blacklistedCategories.contains(raidBossCategory.id())) {
            try {
                webhook = WebhookHandler.sendStartRaidWebhook(this);
            } catch (ExecutionException | InterruptedException e) {
                nr.logError("Failed to send raid_start webhook: " + e.getMessage());
            }
        }

        addTask(raidBossLocation.world(), phaseLength * 20L, this::fightPhase);

        if (nr.config().vouchersJoinRaids) {
            if (startedBy != null && startingItem != null) {
                if (addPlayer(startedBy, true)) {
                    ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(startedBy);
                    if (player != null) player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("joined_raid"), this)));
                }
            }
        }
    }

    public void fightPhase() {
        if (participatingPlayers.size() >= minPlayers && !participatingPlayers.isEmpty()) {
            stage = 2;

            bossbarData = nr.bossbarsConfig().getBossbar(bossInfo, "fight");
            showBossbar(bossbarData);

            phaseLength = bossInfo.raidDetails().fightPhaseTime();
            phaseStartTime = nr.server().getOverworld().getTime();
            fightStartTime = phaseStartTime;

            participatingBroadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("start_fight_phase"), this)));

            if (WebhookHandler.webhookToggle && webhook != 0 && WebhookHandler.runningEmbedEnabled) {
                try {
                    webhook = WebhookHandler.sendRunningWebhook(webhook, this);
                } catch (ExecutionException | InterruptedException e) {
                    nr.logError("Failed to send raid_running webhook: " + e.getMessage());
                }
            }

            addTask(raidBossLocation.world(), phaseLength * 20L, this::raidLost);
        } else {
            stage = -1;
            participatingBroadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("not_enough_players"), this)));
            if (raidBossCategory.requirePass()) {
                if (startingItem != null) {
                    ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(startedBy);
                    if (player != null) {
                        player.giveItemStack(startingItem);
                    }
                }
            }
            if (WebhookHandler.webhookToggle && webhook != 0 && WebhookHandler.deleteIfNoFightPhase) {
                try {
                    WebhookHandler.deleteWebhook(webhook);
                } catch (ExecutionException | InterruptedException e) {
                    nr.logError("Failed to delete webhook: " + e.getMessage());
                }
            }
        }
    }

    public void raidLost() {
        stage = -1;
        tasks.clear();
        raidEndTime = nr.server().getOverworld().getTime();
        participatingBroadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("out_of_time"), this)));
        if (WebhookHandler.webhookToggle && WebhookHandler.failedEmbedEnabled && webhook != 0) {
            try {
                WebhookHandler.sendFailedWebhook(webhook, this);
            } catch (ExecutionException | InterruptedException e) {
                nr.logError("Failed to send raid_failed webhook: " + e.getMessage());
            }
        }
    }

    public void preCatchPhase() {
        stage = 3;

        if (bossInfo.raidDetails().doCatchPhase()) {
            bossbarData = nr.bossbarsConfig().getBossbar(bossInfo, "pre_catch");
            showBossbar(bossbarData);

            phaseLength = bossInfo.raidDetails().preCatchPhaseTime();
        }
        phaseStartTime = nr.server().getOverworld().getTime();
        fightEndTime = phaseStartTime;

        tasks.clear();

        endBattles();

        raidBossEntity.kill();
        handleRewards();

        try {
            nr.config().writeResults(this);
        } catch (IOException | NoSuchElementException e) {
            nr.logError("Failed to write raid information to history file.");
        }

        if (WebhookHandler.webhookToggle && WebhookHandler.endEmbedEnabled && webhook != 0) {
            try {
                WebhookHandler.sendEndRaidWebhook(webhook, this);
            } catch (ExecutionException | InterruptedException e) {
                nr.logError("Failed to send raid_end webhook: " + e.getMessage());
            }
        }

        if (bossInfo.raidDetails().doCatchPhase()) {
            participatingBroadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("catch_phase_warning"), this)));
            addTask(raidBossLocation.world(), phaseLength * 20L, this::catchPhase);
        } else {
            raidWon();
        }
    }

    public void catchPhase() {
        stage = 4;

        bossbarData = nr.bossbarsConfig().getBossbar(bossInfo, "catch");
        showBossbar(bossbarData);

        phaseLength = bossInfo.raidDetails().catchPhaseTime();
        phaseStartTime = nr.server().getOverworld().getTime();

        List<ServerPlayerEntity> alreadyCatching = new ArrayList<>();
        for (CatchPlacement placement : bossInfo.catchSettings().catchPlacements()) {
            List<ServerPlayerEntity> playersToReward = new ArrayList<>();
            if (StringUtils.isNumeric(placement.place())) {
                int placeIndex = Integer.parseInt(placement.place());
                placeIndex--;
                if (placeIndex >= 0 && placeIndex < getDamageLeaderboard().size()) {
                    ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(getDamageLeaderboard().get(placeIndex).getKey());
                    if (player != null) {
                        if (!alreadyCatching.contains(player)) {
                            if (!placement.requireDamage() || (damageByPlayer.containsKey(player.getUuid()) && damageByPlayer.get(player.getUuid()) > 0)) {
                                playersToReward.add(player);
                            }
                        }
                    }
                }
            } else if (placement.place().contains("%")) {
                String percentStr = placement.place().replace("%", "");
                if (StringUtils.isNumeric(percentStr)) {
                    int percent = Integer.parseInt(percentStr);
                    double positions = getDamageLeaderboard().size() * ((double) percent / 100);
                    for (int i = 0; i < ((int) positions); i++) {
                        ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(getDamageLeaderboard().get(i).getKey());
                        if (player != null) {
                            if (!alreadyCatching.contains(player)) {
                                if (!placement.requireDamage() || (damageByPlayer.containsKey(player.getUuid()) && damageByPlayer.get(player.getUuid()) > 0)) {
                                    playersToReward.add(player);
                                }
                            }
                        }
                    }
                }
            } else if (placement.place().equalsIgnoreCase("participating")) {
                for (UUID participatingUUID : participatingPlayers) {
                    ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(participatingUUID);
                    if (player != null) {
                        if (!alreadyCatching.contains(player)) {
                            boolean valid = false;
                            if (placement.requireDamage()) {
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
                    BattleManager.invokeCatchEncounter(this, player, (float) placement.shinyChance(), placement.minPerfectIvs());
                }
            }
        }

        participatingBroadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("start_catch_phase"), this)));

        addTask(raidBossLocation.world(), phaseLength * 20L, this::raidWon);
    }

    public void raidWon() {
        stage = -1;
        tasks.clear();

        raidEndTime = nr.server().getOverworld().getTime();
        if (bossInfo.raidDetails().doCatchPhase()) {
            participatingBroadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("catch_phase_end"), this)));
        }
        participatingBroadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("raid_end"), this)));
    }

    public void handleRewards() {
        participatingBroadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("leaderboard_message_header"), this)));
        int placeIndex = 0;
        for (Map.Entry<String, Integer> entry : getDamageLeaderboard()) {
            ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                placeIndex++;
                participatingBroadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("leaderboard_message_item"), this, player, entry.getValue(), placeIndex)));
                if (placeIndex == 10) {
                    break;
                }
            }
        }
        placeIndex = 0;
        for (Map.Entry<String, Integer> entry : getDamageLeaderboard()) {
            ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                placeIndex++;
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("leaderboard_individual"), this, player, entry.getValue(), placeIndex)));
            }
        }

        List<DistributionSection> categoryRewards = new ArrayList<>(raidBossCategory.rewards());
        List<DistributionSection> bossRewards = new ArrayList<>(bossInfo.raidDetails().rewards());

        List<DistributionSection> rewards = new ArrayList<>(bossRewards);

        if (!bossInfo.raidDetails().overrideCategoryDistribution()) {
            List<Place> overriddenPlacements = new ArrayList<>();

            for (DistributionSection bossReward : bossRewards) {
                List<Place> places = bossReward.places();
                for (Place place : places) {
                    if (place.overrideCategoryReward()) {
                        overriddenPlacements.add(place);
                    }
                }
            }

            for (DistributionSection categoryReward : categoryRewards) {
                boolean overridden = false;
                List<Place> places = categoryReward.places();
                outer:
                for (Place place : places) {
                    for (Place overriddenPlacement : overriddenPlacements) {
                        if (overriddenPlacement.place().equalsIgnoreCase(place.place())) {
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
        for (DistributionSection reward : rewards) {
            List<Place> places = reward.places();
            for (Place place : places) {
                List<ServerPlayerEntity> playersToReward = new ArrayList<>();
                if (StringUtils.isNumeric(place.place())) {
                    int placeAsInt = Integer.parseInt(place.place());
                    placeAsInt--;
                    if (placeAsInt >= 0 && placeAsInt < getDamageLeaderboard().size()) {
                        ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(getDamageLeaderboard().get(placeAsInt).getKey());
                        if (player != null) {
                            if (damageByPlayer.containsKey(player.getUuid())) {
                                if (!place.requireDamage() || damageByPlayer.get(player.getUuid()) > 0) {
                                    playersToReward.add(player);
                                }
                            }
                        }
                    }
                } else if (place.place().contains("%")) {
                    String percentStr = place.place().replace("%", "");
                    if (StringUtils.isNumeric(percentStr)) {
                        int percent = Integer.parseInt(percentStr);
                        double positions = getDamageLeaderboard().size() * ((double) percent / 100);
                        for (int i = 0; i < ((int) Math.ceil(positions)); i++) {
                            ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(getDamageLeaderboard().get(i).getKey());
                            if (player != null) {
                                if (damageByPlayer.containsKey(player.getUuid())) {
                                    if (!place.requireDamage() || damageByPlayer.get(player.getUuid()) > 0) {
                                        playersToReward.add(player);
                                    }
                                }
                            }
                        }
                    }
                } else if (place.place().equalsIgnoreCase("participating")) {
                    for (UUID participatingUUID : participatingPlayers) {
                        ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(participatingUUID);
                        if (player != null) {
                            boolean valid = false;
                            if (place.requireDamage()) {
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
                        for (DistributionSection rewardSection : rewards) {
                            List<Place> rewardPlaces = rewardSection.places();
                            for (Place rewardPlace : rewardPlaces) {
                                if (rewardPlace.place().equalsIgnoreCase(place.place())) {
                                    placeCount++;
                                    break;
                                }
                            }
                            if (placeCount >= 2) {
                                duplicatePlacementExists = true;
                                break;
                            }
                        }

                        if (!noMoreRewards.containsKey(player) || (duplicatePlacementExists && place.place().equalsIgnoreCase(noMoreRewards.get(player)))) {
                            int rolls = new Random().nextInt(reward.minRolls(), reward.maxRolls() + 1);
                            List<UUID> distributedPools = new ArrayList<>();
                            for (int i = 0; i < rolls; i++) {
                                Map.Entry<?, Double> poolEntry = RandomUtils.getRandomEntry(reward.pools());
                                if (poolEntry != null) {
                                    RewardPool pool = (RewardPool) poolEntry.getKey();
                                    if (reward.allowDuplicates() || !distributedPools.contains(pool.uuid())) {
                                        pool.distributeRewards(player);
                                        distributedPools.add(pool.uuid());
                                    } else {
                                        i--;
                                    }
                                } else {
                                    nr.logError("Pool was null!");
                                }
                            }
                        }
                    }
                }

                for (ServerPlayerEntity player : playersToReward) {
                    if (!place.allowOtherRewards() && !noMoreRewards.containsKey(player)) {
                        noMoreRewards.put(player, place.place());
                    }
                }
            }
        }
    }

    public void addTask(ServerWorld world, Long delay, Runnable action) {
        long currentTick = NovaRaids.INSTANCE.server().getOverworld().getTime();
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

    public Map<Long, List<Task>> getTasks() {
        return tasks;
    }

    public void fixBossPosition() {
        if (stage != -1 && stage != 0) {
            if (raidBossEntity != null) {
                if (raidBossEntity.getPos() != raidBossLocation.pos()) {
                    raidBossEntity.teleport(raidBossLocation.world(), raidBossLocation.pos().x, raidBossLocation.pos().y, raidBossLocation.pos().z, null, raidBossLocation.bossFacingDirection(), 0);
                }
            }
        }
    }

    private PokemonEntity generateBossEntity() {
        ServerWorld world = raidBossLocation.world();
        Vec3d pos = raidBossLocation.pos();
        return raidBossPokemonUncatchable.sendOut(world, pos, null, entity -> {
            entity.setPersistent();
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 999999, 9999, true, false));
            entity.setMovementSpeed(0.0f);
            entity.setNoGravity(true);
            entity.setAiDisabled(true);
            if (bossInfo.applyGlowing()) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 999999, 9999, true, false));
            }
            entity.setInvulnerable(true);
            entity.setBodyYaw(raidBossLocation.bossFacingDirection());
            entity.setDrops(new DropTable());
            Box hitbox = entity.getBoundingBox();
            hitbox.stretch(new Vec3d(raidBossPokemonUncatchable.getScaleModifier(), raidBossPokemonUncatchable.getScaleModifier(), raidBossPokemonUncatchable.getScaleModifier()));
            entity.setBoundingBox(hitbox);
            return Unit.INSTANCE;
        });
    }

    private void endBattles() {
        for (UUID playerUUID : participatingPlayers) {
            ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(playerUUID);
            if (player != null) {
                PokemonBattle battle = BattleRegistry.getBattleByParticipatingPlayer(player);
                if (battle != null) {
                    battle.stop();
                }
            }
        }
    }

    public UUID uuid() {
        return uuid;
    }

    public int stage() {
        return stage;
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

    public int maxPlayers() {
        return maxPlayers;
    }

    public int minPlayers() {
        return minPlayers;
    }

    public long raidStartTime() {
        return raidStartTime;
    }

    public long raidEndTime() {
        return raidEndTime;
    }

    public long raidCompletionTime() {
        if (raidEndTime() > 0) {
            return raidEndTime() - raidStartTime();
        }
        return 0;
    }

    public long raidTimer() {
        return nr.server().getOverworld().getTime() - raidStartTime;
    }

    public long bossDefeatTime() {
        return fightEndTime - fightStartTime;
    }

    public BossbarData bossbarData() {
        return bossbarData;
    }

    public long phaseStartTime() {
        return phaseStartTime;
    }

    public long phaseLength() {
        return phaseLength;
    }

    public long phaseEndTime() {
        return phaseStartTime + (phaseLength * 20L);
    }

    public Boss bossInfo() {
        return bossInfo;
    }

    public Pokemon raidBossPokemon() {
        return raidBossPokemon;
    }

    public Pokemon raidBossPokemonUncatchable() {
        return raidBossPokemonUncatchable;
    }

    public String baseGimmick() {
        return baseGimmick;
    }

    public Category raidBossCategory() {
        return raidBossCategory;
    }

    public Location raidBossLocation() {
        return raidBossLocation;
    }

    public int currentHealth() {
        return currentHealth;
    }

    public void applyDamage(int damage) {
        currentHealth -= damage;
    }

    public int maxHealth() {
        return maxHealth;
    }

    public void broadcast(Text text) {
        nr.server().getPlayerManager().getPlayerList().forEach(p -> p.sendMessage(text));
    }

    public void participatingBroadcast(Text text) {
        for (UUID playerUuid : participatingPlayers) {
            ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(playerUuid);
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
                ServerWorld world = nr.server().getOverworld();
                for (ServerWorld worldLoop : nr.server().getWorlds()) {
                    if (worldLoop.getRegistryKey().equals(clone.getWorld().getRegistryKey())) {
                        world = worldLoop;
                    }
                }

                world.setChunkForced(chunkX, chunkZ, true);
                if (!fromFlee) {
                    if (clone.isBattling() && clone.getBattleId() != null) {
                        PokemonBattle battle = BattleRegistry.getBattle(clone.getBattleId());
                        if (battle != null) {
                            battle.stop();
                        }
                    }
                }

                clone.kill();
                world.setChunkForced(chunkX, chunkZ, false);
            }
        }
        clones.remove(clone);
    }

    public Map<PokemonEntity, UUID> getClones() {
        return clones;
    }

    public List<UUID> participatingPlayers() {
        return participatingPlayers;
    }

    public int getPlayerIndex(UUID playerUUID) {
        int index;
        for (index = 0; index < participatingPlayers.size(); index++) {
            if (participatingPlayers.get(index).equals(playerUUID)) {
                return index;
            }
        }
        return -1;
    }

    public void removePlayer(UUID playerUUID) {
        int index = getPlayerIndex(playerUUID);
        if (index != -1) {
            ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(playerUUID);
            if (player != null) {
                markForDeletion.add(playerUUID);
                player.hideBossBar(bossbars().get(playerUUID));
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
    }

    public void removePlayers() {
        if (clearToDelete) {
            if (stage == 1) {
                for (UUID ignored : markForDeletion) {
                    maxHealth = Math.max(maxHealth - bossInfo.healthIncreasePerPlayer(), bossInfo.baseHealth());
                    currentHealth = Math.max(currentHealth - bossInfo.healthIncreasePerPlayer(), bossInfo.baseHealth());
                }
            }

            participatingPlayers().removeAll(markForDeletion);
            markForDeletion.clear();
        }
    }

    public long getCurrentWebhookID() {
        return webhook;
    }

    public boolean addPlayer(UUID playerUUID, boolean usedPass) {
        int index = getPlayerIndex(playerUUID);

        ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(playerUUID);
        if (player != null) {
            if (!Permissions.check(player, "novaraids.override")) {
                for (Raid raid : nr.activeRaids().values()) {
                    index = raid.getPlayerIndex(playerUUID);
                    if (index != -1) {
                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_already_joined_raid"), this)));
                        return false;
                    }
                }

                if (raidBossCategory().requirePass() && !usedPass) {
                    index = -2;
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_no_pass"), this)));
                }

                if (stage != 1) {
                    index = -2;
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_not_joinable"), this)));
                }

                if (BanHandler.hasContraband(player, bossInfo)) {
                    index = -2;
                }

                int numPokemon = 0;
                for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) {
                    if (pokemon != null) {
                        numPokemon++;
                        if (pokemon.getLevel() < bossInfo.raidDetails().minimumLevel()) {
                            index = -2;
                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_minimum_level"), this)));
                            break;
                        }
                        if (pokemon.getLevel() > bossInfo.raidDetails().maximumLevel()) {
                            index = -2;
                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_maximum_level"), this)));
                        }
                    }
                }

                if (numPokemon == 0) {
                    index = -2;
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_no_pokemon"), this)));
                }
            } else {
                nr.logInfo("Player has permission override!");
            }

            if (index == -1) {
                participatingPlayers().add(playerUUID);
                if (participatingPlayers().size() > 1) {
                    maxHealth += bossInfo.healthIncreasePerPlayer();
                    currentHealth += bossInfo.healthIncreasePerPlayer();
                }

                showBossbar(bossbarData);
                if (raidBossLocation.useSetJoinLocation()) {
                    player.teleport(raidBossLocation.world(), raidBossLocation.joinLocation().x, raidBossLocation.joinLocation().y, raidBossLocation.joinLocation().z, raidBossLocation.yaw(), raidBossLocation().pitch());
                }
            } else if (index != -2) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_already_joined_raid"), this)));
            }
            return index == -1;
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
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
        ).values().stream().collect(
                Collectors.groupingBy(value -> value, Collectors.counting())
        );

        List<Integer> duplicates = damageFrequencies.entrySet().stream().filter(
                entry -> entry.getValue() > 1
        ).map(Map.Entry::getKey).toList();

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
        UserCache cache = nr.server().getUserCache();
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

    public boolean isPlayerFleeing(UUID playerUUID) {
        return fleeingPlayers.contains(playerUUID);
    }

    public void addFleeingPlayer(UUID playerUUID) {
        fleeingPlayers.add(playerUUID);
    }

    public void removeFleeingPlayer(UUID playerUUID) {
        fleeingPlayers.remove(playerUUID);
    }

    public Map<UUID, BossBar> bossbars() {
        return playerBossbars;
    }

    private void showBossbar(BossbarData bossbar) {
        hideBossbar();
        if (bossbar != null) {
            for (UUID playerUUID : participatingPlayers) {
                clearToDelete = false;
                ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(playerUUID);
                if (player != null) {
                    BossBar bar = bossbar.createBossBar(this);
                    player.showBossBar(bar);
                    playerBossbars.put(playerUUID, bar);
                }
            }
            clearToDelete = true;
        }
    }

    private void hideBossbar() {
        for (UUID playerUUID : playerBossbars.keySet()) {
            ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(playerUUID);
            if (player != null) {
                player.hideBossBar(playerBossbars.get(playerUUID));
            }
        }
        playerBossbars.clear();
    }

    public void showOverlay(BossbarData bossbar) {
        if (bossbar != null) {
            if (bossbar.useActionbar()) {
                for (UUID playerUUID : participatingPlayers) {
                    clearToDelete = false;
                    ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(playerUUID);
                    if (player != null) {
                        player.sendActionBar(TextUtils.deserialize(TextUtils.parse(bossbar.actionbarText(), this)));
                    }
                }
                clearToDelete = true;
            }
        }
    }
}