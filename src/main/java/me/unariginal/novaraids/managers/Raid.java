package me.unariginal.novaraids.managers;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.drop.DropTable;
import com.cobblemon.mod.common.battles.BattleRegistry;
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
    private final Boss boss_info;
    private final Pokemon raidBoss_pokemon;
    private final Pokemon raidBoss_pokemon_uncatchable;
    private final PokemonEntity raidBoss_entity;
    private final Location raidBoss_location;
    private final Category raidBoss_category;

    private int current_health;
    private int max_health;

    private final UUID started_by;
    private final ItemStack starting_item;

    private final int min_players;
    private final int max_players;
    private final List<UUID> participating_players = new ArrayList<>();
    private final List<UUID> markForDeletion = new ArrayList<>();
    private boolean clearToDelete = true;
    private final Map<UUID, Integer> damage_by_player = new HashMap<>();
    private final List<UUID> latest_damage = new ArrayList<>();

    private final Map<Long, List<Task>> tasks = new HashMap<>();
    private final Map<UUID, BossBar> player_bossbars = new HashMap<>();

    private final Map<PokemonEntity, UUID> clones = new HashMap<>();

    private long raid_start_time = 0;
    private long raid_end_time = 0;
    private long phase_length;
    private long phase_start_time;
    private long fight_start_time;
    private long fight_end_time;
    private BossbarData bossbar_data;

    private long webhook = 0;

    private int stage;

    public Raid(Boss boss_info, Location raidBoss_location, UUID started_by, ItemStack starting_item) {
        this.boss_info = boss_info;
        this.raidBoss_location = raidBoss_location;
        this.started_by = started_by;
        this.starting_item = starting_item;
        if (starting_item != null) {
            starting_item.setCount(1);
        }

        raidBoss_pokemon = boss_info.pokemonDetails().createPokemon();
        raidBoss_pokemon_uncatchable = boss_info.pokemonDetails().createPokemon();
        raidBoss_pokemon_uncatchable.getCustomProperties().add(UncatchableProperty.INSTANCE.uncatchable());
        raidBoss_entity = generate_boss_entity();
        raidBoss_entity.setBodyYaw(raidBoss_location.boss_facing_direction());
        uuid = raidBoss_entity.getUuid();

        max_health = boss_info.base_health();
        current_health = max_health;

        raidBoss_category = nr.bossesConfig().getCategory(boss_info.category_id());
        min_players = raidBoss_category.min_players();
        max_players = raidBoss_category.max_players();

        stage = 0;
        raid_start_time = nr.server().getOverworld().getTime();
        setup_phase();
    }

    public void stop() {
        stage = -1;

        if (raidBoss_entity != null && raidBoss_entity.isAlive() && !raidBoss_entity.isRemoved()) {
            raidBoss_entity.kill();
        }

        end_battles();

        List<PokemonEntity> toRemove = new ArrayList<>(clones.keySet());
        for (PokemonEntity pokemon : toRemove) {
            remove_clone(pokemon);
        }

        for (UUID player_uuid : player_bossbars.keySet()) {
            ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(player_uuid);
            if (player != null) {
                player.hideBossBar(player_bossbars.get(player_uuid));
            }
        }

        raid_end_time = nr.server().getOverworld().getTime();
        nr.init_next_raid();
    }

    public void setup_phase() {
        stage = 1;

        bossbar_data = nr.bossbarsConfig().getBossbar(boss_info, "setup");
        show_bossbar(bossbar_data);

        phase_length = boss_info.raid_details().setup_phase_time();
        phase_start_time = nr.server().getOverworld().getTime();

        broadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("start_pre_phase"), this)));
        nr.messagesConfig().execute_command(this);

        if (WebhookHandler.webhook_toggle && WebhookHandler.start_embed_enabled) {
            try {
                webhook = WebhookHandler.sendStartRaidWebhook(this);
            } catch (ExecutionException | InterruptedException e) {
                nr.logError("Failed to send raid_start webhook: " + e.getMessage());
            }
        }

        addTask(raidBoss_location.world(), phase_length * 20L, this::fight_phase);
    }

    public void fight_phase() {
        if (participating_players.size() >= min_players && !participating_players.isEmpty()) {
            stage = 2;

            bossbar_data = nr.bossbarsConfig().getBossbar(boss_info, "fight");
            show_bossbar(bossbar_data);

            phase_length = boss_info.raid_details().fight_phase_time();
            phase_start_time = nr.server().getOverworld().getTime();
            fight_start_time = phase_start_time;

            participating_broadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("start_fight_phase"), this)));

            if (WebhookHandler.webhook_toggle && webhook != 0 && WebhookHandler.running_embed_enabled) {
                try {
                    webhook = WebhookHandler.sendRunningWebhook(webhook, this);
                } catch (ExecutionException | InterruptedException e) {
                    nr.logError("Failed to send raid_running webhook: " + e.getMessage());
                }
            }

            addTask(raidBoss_location.world(), phase_length * 20L, this::raid_lost);
        } else {
            stage = -1;
            participating_broadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("not_enough_players"), this)));
            if (raidBoss_category.require_pass()) {
                if (starting_item != null) {
                    ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(started_by);
                    if (player != null) {
                        player.giveItemStack(starting_item);
                    }
                }
            }
        }
    }

    public void raid_lost() {
        stage = -1;
        tasks.clear();
        raid_end_time = nr.server().getOverworld().getTime();
        participating_broadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("out_of_time"), this)));
    }

    public void pre_catch_phase() {
        stage = 3;

        if (boss_info.raid_details().do_catch_phase()) {
            bossbar_data = nr.bossbarsConfig().getBossbar(boss_info, "pre_catch");
            show_bossbar(bossbar_data);

            phase_length = boss_info.raid_details().pre_catch_phase_time();
        }
        phase_start_time = nr.server().getOverworld().getTime();
        fight_end_time = phase_start_time;

        tasks.clear();

        end_battles();

        raidBoss_entity.kill();
        handle_rewards();

        try {
            nr.config().writeResults(this);
        } catch (IOException | NoSuchElementException e) {
            nr.logError("[RAIDS] Failed to write raid information to history file.");
        }

        if (WebhookHandler.webhook_toggle && WebhookHandler.end_embed_enabled && webhook != 0) {
            try {
                WebhookHandler.sendEndRaidWebhook(webhook, this);
            } catch (ExecutionException | InterruptedException e) {
                nr.logError("Failed to send start_raid webhook: " + e.getMessage());
            }
        }

        if (boss_info.raid_details().do_catch_phase()) {
            participating_broadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("catch_phase_warning"), this)));
            addTask(raidBoss_location.world(), phase_length * 20L, this::catch_phase);
        } else {
            raid_won();
        }
    }

    public void catch_phase() {
        stage = 4;

        bossbar_data = nr.bossbarsConfig().getBossbar(boss_info, "catch");
        show_bossbar(bossbar_data);

        phase_length = boss_info.raid_details().catch_phase_time();
        phase_start_time = nr.server().getOverworld().getTime();

        List<ServerPlayerEntity> already_catching = new ArrayList<>();
        for (CatchPlacement placement : boss_info.catch_settings().catch_placements()) {
            List<ServerPlayerEntity> players_to_reward = new ArrayList<>();
            if (StringUtils.isNumeric(placement.place())) {
                int placeIndex = Integer.parseInt(placement.place());
                placeIndex--;
                if (placeIndex >= 0 && placeIndex < get_damage_leaderboard().size()) {
                    ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(get_damage_leaderboard().get(placeIndex).getKey());
                    if (player != null) {
                        if (!already_catching.contains(player)) {
                            if (!placement.require_damage() || (damage_by_player.containsKey(player.getUuid()) && damage_by_player.get(player.getUuid()) > 0)) {
                                players_to_reward.add(player);
                            }
                        }
                    }
                }
            } else if (placement.place().contains("%")) {
                String percentStr = placement.place().replace("%", "");
                if (StringUtils.isNumeric(percentStr)) {
                    int percent = Integer.parseInt(percentStr);
                    double positions = get_damage_leaderboard().size() * ((double) percent / 100);
                    for (int i = 0; i < ((int) positions); i++) {
                        ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(get_damage_leaderboard().get(i).getKey());
                        if (player != null) {
                            if (!already_catching.contains(player)) {
                                if (!placement.require_damage() || (damage_by_player.containsKey(player.getUuid()) && damage_by_player.get(player.getUuid()) > 0)) {
                                    players_to_reward.add(player);
                                }
                            }
                        }
                    }
                }
            } else if (placement.place().equalsIgnoreCase("participating")) {
                for (Map.Entry<String, Integer> entry : get_damage_leaderboard()) {
                    ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(entry.getKey());
                    if (player != null) {
                        if (!already_catching.contains(player)) {
                            if (!placement.require_damage() || (damage_by_player.containsKey(player.getUuid()) && damage_by_player.get(player.getUuid()) > 0)) {
                                players_to_reward.add(player);
                            }
                        }
                    }
                }
            }

            for (ServerPlayerEntity player : players_to_reward) {
                if (player != null) {
                    already_catching.add(player);
                    BattleManager.invoke_catch_encounter(this, player, (float) placement.shiny_chance(), placement.min_perfect_ivs());
                }
            }
        }

        participating_broadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("start_catch_phase"), this)));

        addTask(raidBoss_location.world(), phase_length * 20L, this::raid_won);
    }

    public void raid_won() {
        stage = -1;
        tasks.clear();

        raid_end_time = nr.server().getOverworld().getTime();
        if (boss_info.raid_details().do_catch_phase()) {
            participating_broadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("catch_phase_end"), this)));
        }
        participating_broadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("raid_end"), this)));
    }

    public void handle_rewards() {
        participating_broadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("leaderboard_message_header"), this)));
        int place_index = 0;
        for (Map.Entry<String, Integer> entry : get_damage_leaderboard()) {
            ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                place_index++;
                participating_broadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("leaderboard_message_item"), this, player, entry.getValue(), place_index)));
                if (place_index == 10) {
                    break;
                }
            }
        }
        place_index = 0;
        for (Map.Entry<String, Integer> entry : get_damage_leaderboard()) {
            ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                place_index++;
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("leaderboard_individual"), this, player, entry.getValue(), place_index)));
            }
        }

        // TODO: <!>TESTING<!> Use BOTH reward distribution sections!
        List<DistributionSection> category_rewards = raidBoss_category.rewards();
        List<DistributionSection> boss_rewards = boss_info.raid_details().rewards();

        List<Place> overridden_placements = new ArrayList<>();

        for (DistributionSection boss_reward : boss_rewards) {
            List<Place> places = boss_reward.places();
            for (Place place : places) {
                if (place.override_category_reward()) {
                    overridden_placements.add(place);
                }
            }
        }

        List<DistributionSection> rewards = new ArrayList<>(boss_rewards);

        for (DistributionSection category_reward : category_rewards) {
            boolean overridden = false;
            List<Place> places = category_reward.places();
            for (Place place : places) {
                for (Place overridden_placement : overridden_placements) {
                    if (overridden_placement.place().equalsIgnoreCase(place.place())) {
                        overridden = true;
                    }
                }
            }
            if (!overridden) {
                rewards.add(category_reward);
            }
        }

        List<ServerPlayerEntity> no_more_rewards = new ArrayList<>();
        for (DistributionSection reward : rewards) {
            List<Place> places = reward.places();
            for (Place place : places) {
                List<ServerPlayerEntity> players_to_reward = new ArrayList<>();
                if (StringUtils.isNumeric(place.place())) {
                    int placeIndex = Integer.parseInt(place.place());
                    placeIndex--;
                    if (placeIndex >= 0 && placeIndex < get_damage_leaderboard().size()) {
                        ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(get_damage_leaderboard().get(placeIndex).getKey());
                        if (player != null) {
                            if (damage_by_player.containsKey(player.getUuid())) {
                                if (!place.require_damage() || damage_by_player.get(player.getUuid()) > 0) {
                                    players_to_reward.add(player);
                                }
                            }
                        }
                    }
                } else if (place.place().contains("%")) {
                    String percentStr = place.place().replace("%", "");
                    if (StringUtils.isNumeric(percentStr)) {
                        int percent = Integer.parseInt(percentStr);
                        double positions = get_damage_leaderboard().size() * ((double) percent / 100);
                        for (int i = 0; i < ((int) Math.ceil(positions)); i++) {
                            ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(get_damage_leaderboard().get(i).getKey());
                            if (player != null) {
                                if (damage_by_player.containsKey(player.getUuid())) {
                                    if (!place.require_damage() || damage_by_player.get(player.getUuid()) > 0) {
                                        players_to_reward.add(player);
                                    }
                                }
                            }
                        }
                    }
                } else if (place.place().equalsIgnoreCase("participating")) {
                    for (Map.Entry<String, Integer> entry : get_damage_leaderboard()) {
                        ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(entry.getKey());
                        if (player != null) {
                            if (damage_by_player.containsKey(player.getUuid())) {
                                if (!place.require_damage() || damage_by_player.get(player.getUuid()) > 0) {
                                    players_to_reward.add(player);
                                }
                            }
                        }
                    }
                }

                for (ServerPlayerEntity player : players_to_reward) {
                    if (player != null) {
                        boolean duplicate_placement_exists = false;
                        outer:
                        for (DistributionSection rewardSection : rewards) {
                            List<Place> rewardPlaces = rewardSection.places();
                            for (Place rewardPlace : rewardPlaces) {
                                if (rewardPlace.place().equalsIgnoreCase(place.place())) {
                                    duplicate_placement_exists = true;
                                    break outer;
                                }
                            }
                        }

                        if (!no_more_rewards.contains(player) || duplicate_placement_exists) {
                            int rolls = new Random().nextInt(reward.min_rolls(), reward.max_rolls());
                            List<RewardPool> distributed_pools = new ArrayList<>();
                            for (int i = 0; i < rolls; i++) {
                                Map.Entry<?, Double> pool_entry = RandomUtils.getRandomEntry(reward.pools());
                                if (pool_entry != null) {
                                    RewardPool pool = (RewardPool) pool_entry.getKey();
                                    if (reward.allow_duplicates() || !distributed_pools.contains(pool)) {
                                        pool.distributeRewards(player);
                                        distributed_pools.add(pool);
                                    }
                                }
                            }
                        }
                    }
                }

                for (ServerPlayerEntity player : players_to_reward) {
                    if (!place.allow_other_rewards()) {
                        no_more_rewards.add(player);
                    }
                }
            }
        }
    }

    public void addTask(ServerWorld world, Long delay, Runnable action) {
        long current_tick = world.getTime();
        long execute_tick = current_tick + delay;

        Task task = new Task(world, execute_tick, action);
        if (tasks.containsKey(execute_tick)) {
            List<Task> task_list = tasks.get(execute_tick);
            task_list.add(task);
            tasks.replace(execute_tick, task_list);
        } else {
            tasks.put(execute_tick, List.of(task));
        }
    }

    public void removeTask(long execute_tick) {
        tasks.remove(execute_tick);
    }

    public Map<Long, List<Task>> getTasks() {
        return tasks;
    }

    public void fixBossPosition() {
        if (stage != -1 && stage != 0) {
            if (raidBoss_entity != null) {
                if (raidBoss_entity.getPos() != raidBoss_location.pos()) {
                    raidBoss_entity.teleport(raidBoss_location.world(), raidBoss_location.pos().x, raidBoss_location.pos().y, raidBoss_location.pos().z, null, raidBoss_location.boss_facing_direction(), 0);
                }
            }
        }
    }

    private PokemonEntity generate_boss_entity() {
        ServerWorld world = raidBoss_location.world();
        Vec3d pos = raidBoss_location.pos();
        return raidBoss_pokemon_uncatchable.sendOut(world, pos, null, entity -> {
            entity.setPersistent();
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, -1, 9999, true, false));
            entity.setMovementSpeed(0.0f);
            entity.setNoGravity(true);
            entity.setAiDisabled(true);
            if (boss_info.apply_glowing()) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, -1, 9999, true, false));
            }
            entity.setInvulnerable(true);
            entity.setBodyYaw(raidBoss_location.boss_facing_direction());
            entity.setDrops(new DropTable());
            Box hitbox = entity.getBoundingBox();
            hitbox.stretch(new Vec3d(raidBoss_pokemon_uncatchable.getScaleModifier(), raidBoss_pokemon_uncatchable.getScaleModifier(), raidBoss_pokemon_uncatchable.getScaleModifier()));
            entity.setBoundingBox(hitbox);
            return Unit.INSTANCE;
        });
    }

    private void end_battles() {
        for (UUID player_uuid : participating_players) {
            ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(player_uuid);
            if (player != null) {
                PokemonBattle battle = BattleRegistry.INSTANCE.getBattleByParticipatingPlayer(player);
                if (battle != null) {
                    battle.end();
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

    public String get_phase() {
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

    public int max_players() {
        return max_players;
    }

    public int min_players() {
        return min_players;
    }

    public long raid_start_time() {
        return raid_start_time;
    }

    public long raid_end_time() {
        return raid_end_time;
    }

    public long raid_completion_time() {
        if (raid_end_time() > 0) {
            return raid_end_time() - raid_start_time();
        }
        return 0;
    }

    public long raid_timer() {
        return nr.server().getOverworld().getTime() - raid_start_time;
    }

    public long boss_defeat_time() {
        return fight_end_time - fight_start_time;
    }

    public BossbarData bossbar_data() {
        return bossbar_data;
    }

    public long phase_start_time() {
        return phase_start_time;
    }

    public long phase_length() {
        return phase_length;
    }

    public long phase_end_time() {
        return phase_start_time + (phase_length * 20L);
    }

    public Boss boss_info() {
        return boss_info;
    }

    public Pokemon raidBoss_pokemon() {
        return raidBoss_pokemon;
    }

    public Pokemon raidBoss_pokemon_uncatchable() {
        return raidBoss_pokemon_uncatchable;
    }

    public PokemonEntity raidBoss_entity() {
        return raidBoss_entity;
    }

    public Category raidBoss_category() {
        return raidBoss_category;
    }

    public Location raidBoss_location() {
        return raidBoss_location;
    }

    public int current_health() {
        return current_health;
    }

    public void apply_damage(int damage) {
        current_health -= damage;
    }

    public int max_health() {
        return max_health;
    }

    public void broadcast(Text text) {
        nr.server().getPlayerManager().getPlayerList().forEach(p -> p.sendMessage(text));
    }

    public void participating_broadcast(Text text) {
        for (UUID player_uuid : participating_players) {
            ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(player_uuid);
            if (player != null) {
                player.sendMessage(text);
            }
        }
    }

    public void add_clone(PokemonEntity pokemon, ServerPlayerEntity player) {
        for (PokemonEntity clone : clones.keySet()) {
            if (clone.getUuid().equals(pokemon.getUuid())) {
                return;
            }
        }
        clones.put(pokemon, player.getUuid());
    }

    public void remove_clone(PokemonEntity clone) {
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
                if (clone.isBattling() && clone.getBattleId() != null) {
                    PokemonBattle battle = BattleRegistry.INSTANCE.getBattle(clone.getBattleId());
                    if (battle != null) {
                        battle.end();
                    }
                }

                clone.kill();
                world.setChunkForced(chunkX, chunkZ, false);
            }
        }
        clones.remove(clone);
    }

    public Map<PokemonEntity, UUID> get_clones() {
        return clones;
    }

    public List<UUID> participating_players() {
        return participating_players;
    }

    public int get_player_index(UUID player_uuid) {
        int index;
        for (index = 0; index < participating_players.size(); index++) {
            if (participating_players.get(index).equals(player_uuid)) {
                return index;
            }
        }
        return -1;
    }

    public void removePlayer(UUID player_uuid) {
        int index = get_player_index(player_uuid);
        if (index != -1) {
            ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(player_uuid);
            if (player != null) {
                markForDeletion.add(player_uuid);
                player.hideBossBar(bossbars().get(player_uuid));
                player_bossbars.remove(player_uuid);
                //damage_by_player.remove(player_uuid);

                List<PokemonEntity> toRemove = new ArrayList<>();
                for (PokemonEntity clone : clones.keySet()) {
                    if (clones.get(clone).equals(player.getUuid())) {
                        toRemove.add(clone);
                    }
                }
                for (PokemonEntity clone : toRemove) {
                    remove_clone(clone);
                }
            }
        }
    }

    public void removePlayers() {
        if (clearToDelete) {
            participating_players().removeAll(markForDeletion);
            markForDeletion.clear();
        }
    }

    public long getCurrentWebhookID() {
        return webhook;
    }

    public void editWebhookID(long webhookID) {
        this.webhook = webhookID;
    }

    public boolean addPlayer(UUID player_uuid, boolean usedPass) {
        int index = get_player_index(player_uuid);

        ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(player_uuid);
        if (player != null) {
            if (!Permissions.check(player, "novaraids.override")) {
                for (Raid raid : nr.active_raids().values()) {
                    index = raid.get_player_index(player_uuid);
                    if (index != -1) {
                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_already_joined_raid"), this)));
                        return false;
                    }
                }

                if (raidBoss_category().require_pass() && !usedPass) {
                    index = -2;
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_no_pass"), this)));
                }

                if (stage != 1) {
                    index = -2;
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_not_joinable"), this)));
                }

                if (BanHandler.hasContraband(player, boss_info)) {
                    index = -2;
                }

                int num_pokemon = 0;
                for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) {
                    if (pokemon != null) {
                        num_pokemon++;
                        if (pokemon.getLevel() < boss_info.raid_details().minimum_level()) {
                            index = -2;
                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_minimum_level"), this)));
                            break;
                        }
                    }
                }

                if (num_pokemon == 0) {
                    index = -2;
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_no_pokemon"), this)));
                }
            } else {
                nr.logInfo("Player has permission override!");
            }

            if (index == -1) {
                participating_players().add(player_uuid);
                if (participating_players().size() > 1) {
                    max_health += boss_info.health_increase_per_player();
                    current_health += boss_info.health_increase_per_player();
                }

                show_bossbar(bossbar_data);
                if (raidBoss_location.use_set_join_location()) {
                    player.teleport(raidBoss_location.world(), raidBoss_location.join_location().x, raidBoss_location.join_location().y, raidBoss_location.join_location().z, raidBoss_location.yaw(), raidBoss_location().pitch());
                }
            } else if (index != -2) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_already_joined_raid"), this)));
            }
            return index == -1;
        }
        return false;
    }

    public void update_player_damage(UUID player_uuid, int damage) {
        if (damage_by_player.containsKey(player_uuid)) {
            damage += damage_by_player.get(player_uuid);
        }
        damage_by_player.put(player_uuid, damage);
        latest_damage.remove(player_uuid);
        latest_damage.add(player_uuid);
    }

    public List<Map.Entry<String, Integer>> get_damage_leaderboard() {
        List<Map.Entry<UUID, Integer>> leaderboard_backwards = damage_by_player.entrySet().stream().sorted(Map.Entry.comparingByValue()).toList();
        Map<String, Integer> leaderboard = new HashMap<>();
        for (int i = leaderboard_backwards.size() - 1; i >= 0; i--) {
            Optional<GameProfile> player = Objects.requireNonNull(nr.server().getUserCache()).getByUuid(leaderboard_backwards.get(i).getKey());
            int finalI = i;
            player.ifPresent(gameProfile -> leaderboard.put(gameProfile.getName(), leaderboard_backwards.get(finalI).getValue()));
        }

        Map<Integer, Long> damage_frequencies = leaderboard.values().stream().collect(Collectors.groupingBy(value -> value, Collectors.counting()));
        List<Integer> duplicates = damage_frequencies.entrySet().stream().filter(entry -> entry.getValue() > 1).map(Map.Entry::getKey).toList();

        List<Map.Entry<String, Integer>> backwards_again = leaderboard.entrySet().stream().sorted((e1, e2) -> {
            if (duplicates.contains(e1.getValue()) && duplicates.contains(e2.getValue())) {
                for (UUID uuid1 : latest_damage) {
                    UserCache userCache = nr.server().getUserCache();
                    if (userCache != null) {
                        Optional<GameProfile> profile = userCache.getByUuid(uuid1);
                        if (profile.isPresent()) {
                            if (e1.getKey().equals(profile.get().getName())) {
                                return -1;
                            } else if (e2.getKey().equals(profile.get().getName())) {
                                return 1;
                            }
                        }
                    }
                }
            } else if (duplicates.contains(e1.getValue())) {
                return -1;
            } else if (duplicates.contains(e2.getValue())) {
                return 1;
            }
            return 0;
        }).toList();
        List<Map.Entry<String, Integer>> sorted_leaderboard = new ArrayList<>();
        for (Map.Entry<String, Integer> stringIntegerEntry : backwards_again) {
            sorted_leaderboard.addFirst(stringIntegerEntry);
        }
        return sorted_leaderboard;
    }

    public Map<UUID, BossBar> bossbars() {
        return player_bossbars;
    }

    private void show_bossbar(BossbarData bossbar) {
        hide_bossbar();
        if (bossbar != null) {
            for (UUID player_uuid : participating_players) {
                clearToDelete = false;
                ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(player_uuid);
                if (player != null) {
                    BossBar bar = bossbar.createBossBar(this);
                    player.showBossBar(bar);
                    player_bossbars.put(player_uuid, bar);
                }
            }
            clearToDelete = true;
        }
    }

    private void hide_bossbar() {
        for (UUID player_uuid : player_bossbars.keySet()) {
            ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(player_uuid);
            if (player != null) {
                player.hideBossBar(player_bossbars.get(player_uuid));
            }
        }
        player_bossbars.clear();
    }

    public void show_overlay(BossbarData bossbar) {
        if (bossbar != null) {
            if (bossbar.use_actionbar()) {
                for (UUID player_uuid : participating_players) {
                    clearToDelete = false;
                    ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(player_uuid);
                    if (player != null) {
                        player.sendActionBar(TextUtils.deserialize(TextUtils.parse(bossbar.actionbar_text(), this)));
                    }
                }
                clearToDelete = true;
            }
        }
    }
}
