package me.unariginal.novaraids.managers;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty;
import kotlin.Unit;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.*;
import me.unariginal.novaraids.utils.TextUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class Raid {
    private final NovaRaids nr = NovaRaids.INSTANCE;

    private final UUID uuid;
    private final Boss boss_info;
    private final Pokemon raidBoss_pokemon;
    private final Pokemon raidBoss_pokemon_uncatchable;
    private final PokemonEntity raidBoss_entity;
    private final Location raidBoss_location;
    private final Category raidBoss_category;

    private int current_health;
    private final int max_health;

    private final int min_players;
    private final int max_players;
    private final List<ServerPlayerEntity> participating_players = new ArrayList<>();

    private final Map<Long, List<Task>> tasks = new HashMap<>();
    private final Map<ServerPlayerEntity, BossBar> player_bossbars = new HashMap<>();

    private long phase_length;
    private long phase_start_time;
    private BossbarData bossbar_data;

    private int stage;

    public Raid(Boss boss_info, Location raidBoss_location) {
        this.boss_info = boss_info;
        this.raidBoss_location = raidBoss_location;

        raidBoss_pokemon = boss_info.createPokemon();
        raidBoss_pokemon_uncatchable = boss_info.createPokemon();
        raidBoss_pokemon_uncatchable.getCustomProperties().add(UncatchableProperty.INSTANCE.uncatchable());
        raidBoss_entity = generate_boss_entity();
        uuid = raidBoss_entity.getUuid();

        max_health = boss_info.base_health();
        current_health = max_health;

        raidBoss_category = nr.config().getCategory(boss_info.category());
        min_players = raidBoss_category.min_players();
        max_players = raidBoss_category.max_players();

        stage = 0;
        setup_phase();
    }

    public void stop() {
        stage = -1;

        if (raidBoss_entity != null) {
            raidBoss_entity.remove(Entity.RemovalReason.DISCARDED);
        }

        for (ServerPlayerEntity player : participating_players) {
            PokemonBattle battle = BattleRegistry.INSTANCE.getBattleByParticipatingPlayer(player);
            if (battle != null) {
                battle.end();
            }
        }

        for (Entity entity : raidBoss_location.world().iterateEntities()) {
            if (entity.getPos().distanceTo(raidBoss_location.pos()) <= nr.config().getSettings().raid_radius()) {
                if (entity instanceof PokemonEntity pokemon_entity) {
                    if (pokemon_entity.getPokemon().getPersistentData().contains("boss_clone")) {
                        if (pokemon_entity.getPokemon().getPersistentData().getBoolean("boss_clone")) {
                            pokemon_entity.remove(Entity.RemovalReason.DISCARDED);
                        }
                    }
                }
            }
        }

        for (ServerPlayerEntity player : player_bossbars.keySet()) {
            player.hideBossBar(player_bossbars.get(player));
        }
    }

    private void setup_phase() {
        stage = 1;

        bossbar_data = nr.config().getBossbar("setup", raidBoss_category.name(), boss_info.name());
        show_bossbar(bossbar_data);

        phase_length = nr.config().getSettings().setup_phase_time();
        phase_start_time = nr.server().getOverworld().getTime();

        addTask(raidBoss_location.world(), phase_length * 20L, this::fight_phase);
    }

    private void fight_phase() {
        stage = 2;

        bossbar_data = nr.config().getBossbar("fight", raidBoss_category.name(), boss_info.name());
        show_bossbar(bossbar_data);

        phase_length = nr.config().getSettings().fight_phase_time();
        phase_start_time = nr.server().getOverworld().getTime();

        addTask(raidBoss_location.world(), phase_length * 20L, this::raid_lost);
    }

    private void raid_lost() {
        stage = -1;
        tasks.clear();
    }

    public void pre_catch_phase() {
        stage = 3;

        bossbar_data = nr.config().getBossbar("pre_catch", raidBoss_category.name(), boss_info.name());
        show_bossbar(bossbar_data);

        phase_length = nr.config().getSettings().pre_catch_phase_time();
        phase_start_time = nr.server().getOverworld().getTime();

        tasks.clear();

        end_battles();

        addTask(raidBoss_location.world(), phase_length * 20L, this::catch_phase);
    }

    private void catch_phase() {
        stage = 4;

        bossbar_data = nr.config().getBossbar("catch", raidBoss_category.name(), boss_info.name());
        show_bossbar(bossbar_data);

        phase_length = nr.config().getSettings().catch_phase_time();
        phase_start_time = nr.server().getOverworld().getTime();

        for (ServerPlayerEntity player : participating_players) {
            BattleManager.invoke_catch_encounter(this, player);
        }

        addTask(raidBoss_location.world(), phase_length * 20L, this::raid_won);
    }

    private void raid_won() {
        stage = -1;
        tasks.clear();
    }

    private void addTask(ServerWorld world, Long delay, Runnable action) {
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

    public Map<Long, List<Task>> getTasks() {
        return tasks;
    }

    public void fixBossPosition() {
        if (stage != -1 && stage != 0) {
            if (raidBoss_entity != null) {
                if (raidBoss_entity.getPos() != raidBoss_location.pos()) {
                    raidBoss_entity.teleport(raidBoss_location.pos().x, raidBoss_location.pos().y, raidBoss_location.pos().z, false);
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
            entity.setGlowing(nr.config().getSettings().bosses_glow());
            entity.setInvulnerable(true);
            return Unit.INSTANCE;
        });
    }

    private void end_battles() {
        for (ServerPlayerEntity player : participating_players) {
            PokemonBattle battle = BattleRegistry.INSTANCE.getBattleByParticipatingPlayer(player);
            if (battle != null) {
                battle.end();
            }
        }
    }

    public UUID uuid() {
        return uuid;
    }

    public int stage() {
        return stage;
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

    public List<ServerPlayerEntity> participating_players() {
        return participating_players;
    }

    public int get_player_index(ServerPlayerEntity player) {
        int index;
        for (index = 0; index < participating_players.size(); index++) {
            if (participating_players.get(index).equals(player)) {
                return index;
            }
        }
        return -1;
    }

    public void removePlayer(ServerPlayerEntity player) {
        int index = get_player_index(player);
        if (index != -1) {
            participating_players.remove(index);
        }
    }

    public boolean addPlayer(ServerPlayerEntity player) {
        int index = get_player_index(player);
        if (index == -1) {
            participating_players().add(player);
            show_bossbar(bossbar_data);
        }
        return index == -1;
    }

    public Map<ServerPlayerEntity, BossBar> bossbars() {
        return player_bossbars;
    }

    private void show_bossbar(BossbarData bossbar) {
        hide_bossbar();
        if (bossbar != null) {
            for (ServerPlayerEntity player : participating_players) {
                BossBar bar = bossbar.createBossBar(this);
                player.showBossBar(bar);
                player_bossbars.put(player, bar);
            }
        }
    }

    private void hide_bossbar() {
        for (ServerPlayerEntity player : player_bossbars.keySet()) {
            player.hideBossBar(player_bossbars.get(player));
        }
        player_bossbars.clear();
    }

    public void show_overlay(BossbarData bossbar) {
        if (bossbar.use_overlay()) {
            for (ServerPlayerEntity player : participating_players) {
                player.sendActionBar(nr.mm().deserialize(TextUtil.parse(bossbar.overlay_text(), this)));
            }
        }
    }
}
