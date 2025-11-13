package me.unariginal.novaraids.managers;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.drop.DropTable;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.ShinyChanceCalculationEvent;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.stats.SidemodEvSource;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.api.types.tera.TeraTypes;
import com.cobblemon.mod.common.battles.*;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.battles.ai.StrongBattleAI;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.EVs;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.IVs;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty;
import kotlin.Unit;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.bosssettings.CatchSettings;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class BattleManager {
    public static boolean checkRate(float shinyRate) {
        if (shinyRate >= 1) {
            return (kotlin.random.Random.Default.nextFloat() < (1 / shinyRate));
        } else {
            return (kotlin.random.Random.Default.nextFloat() < shinyRate);
        }
    }

    public static void invokeCatchEncounter(Raid raid, ServerPlayerEntity player, float shinyChance, int minPerfectIvs) {
        Pokemon pokemon = raid.bossInfo().pokemonDetails().createPokemon(true);
        NbtCompound data = new NbtCompound();
        data.putBoolean("raid_entity", true);
        data.putBoolean("boss_clone", true);
        data.putBoolean("catch_encounter", true);
        pokemon.setPersistentData$common(data);

        CatchSettings settings = raid.bossInfo().catchSettings();

        if (settings.speciesOverride() != raid.bossInfo().pokemonDetails().species()) {
            pokemon.setSpecies(settings.speciesOverride());
        }

        if (shinyChance > 0) {
            AtomicReference<Float> newShinyChance = new AtomicReference<>(shinyChance);
            CobblemonEvents.SHINY_CHANCE_CALCULATION.post(new ShinyChanceCalculationEvent[]{new ShinyChanceCalculationEvent(shinyChance, pokemon)}, event -> {
                newShinyChance.set(event.calculate(player));
                return Unit.INSTANCE;
            });
            shinyChance = newShinyChance.get();
            pokemon.setShiny(checkRate(shinyChance));
        } else {
            pokemon.setShiny(false);
        }

        if (!settings.keepScale()) {
            pokemon.setScaleModifier(1.0f);
        }

        if (!settings.keepFeatures()) {
            PokemonProperties.Companion.parse(settings.featuresOverride()).apply(pokemon);
        }

        if (!settings.keepHeldItem()) {
            pokemon.removeHeldItem();
        }

        if (settings.keepEvs()) {
            EVs new_evs = new EVs();
            for (Map.Entry<? extends Stat, ? extends Integer> ev : pokemon.getEvs()) {
                new_evs.add(ev.getKey(), ev.getValue(), new SidemodEvSource(NovaRaids.MOD_ID, pokemon));
            }
            for (Map.Entry<? extends Stat, ? extends Integer> ev : new_evs) {
                pokemon.setEV(ev.getKey(), ev.getValue());
            }
        } else {
            for (Map.Entry<? extends Stat, ? extends Integer> ev : pokemon.getEvs()) {
                pokemon.setEV(ev.getKey(), 0);
            }
        }

        if (settings.randomizeIvs()) {
            IVs new_ivs = IVs.createRandomIVs(minPerfectIvs);
            for (Map.Entry<? extends Stat, ? extends Integer> iv : new_ivs) {
                pokemon.setIV(iv.getKey(), iv.getValue());
            }
        }

        if (settings.randomizeGender()) {
            pokemon.setGender(
                    (pokemon.getForm().getMaleRatio() >= 0F && pokemon.getForm().getMaleRatio() <= 1f) ?
                            Gender.GENDERLESS :
                            (pokemon.getForm().getMaleRatio() == 1F || new Random().nextFloat() < pokemon.getForm().getMaleRatio()) ?
                                    Gender.MALE : Gender.FEMALE
            );
        } else {
            pokemon.setGender(raid.raidBossPokemon().getGender());
        }

        if (settings.randomizeNature()) {
            pokemon.setNature(Natures.getRandomNature());
        } else {
            pokemon.setNature(raid.raidBossPokemon().getNature());
        }

        if (settings.randomizeAbility()) {
            pokemon.updateAbility(pokemon.getForm().getAbilities().select(pokemon.getSpecies(), pokemon.getAspects()).getFirst());
        } else {
            pokemon.updateAbility(raid.raidBossPokemon().getAbility());
        }

        pokemon.setLevel(settings.levelOverride());

        if (settings.resetMoves()) {
            pokemon.getMoveSet().clear();
            Set<MoveTemplate> moves = pokemon.getSpecies().getMoves().getLevelUpMovesUpTo(pokemon.getLevel());
            int slot = 0;
            for (MoveTemplate move : moves) {
                pokemon.getMoveSet().setMove(slot, move.create());
                slot++;
                if (slot > 4) {
                    break;
                }
            }
        }

        pokemon.setFriendship(settings.friendshipOverride(), true);

        pokemon.setDmaxLevel(settings.dmaxLevelOverride());

        if (settings.randomizeTeraType()) {
            pokemon.setTeraType(TeraTypes.random(true));
        }

        if (settings.resetGmaxFactor()) {
            pokemon.setGmaxFactor(false);
        }

        PokemonEntity bossClone = pokemon.sendOut(raid.raidBossLocation().world(), player.getPos().offset(player.getFacing(), 1), null, entity -> {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, -1, 9999, false, false));
            entity.setNoGravity(true);
            entity.setMovementSpeed(0.0f);
            entity.setDrops(new DropTable());
            return Unit.INSTANCE;
        });

        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);

        if (bossClone != null) {
            raid.addClone(bossClone, player);
            pveOverride(player, bossClone, null, BattleFormat.Companion.getGEN_9_SINGLES(), false, raid.bossInfo().raidDetails().healPartyOnChallenge(), Cobblemon.config.getDefaultFleeDistance(), party, raid.bossInfo().aiSkillLevel());
        }
    }

    public static void invokeBattle(Raid raid, ServerPlayerEntity player) {
        Pokemon pokemon = raid.bossInfo().pokemonDetails().createPokemon(false);
        if (!raid.bossInfo().rerollFeaturesEachBattle()) {
            pokemon.setFeatures(raid.raidBossPokemonUncatchable().getFeatures());
        }
        pokemon.getCustomProperties().add(UncatchableProperty.INSTANCE.uncatchable());
        pokemon.setAbility$common(raid.raidBossPokemonUncatchable().getAbility());
        pokemon.setGender(raid.raidBossPokemonUncatchable().getGender());
        pokemon.setNature(raid.raidBossPokemonUncatchable().getNature());
        NbtCompound data = new NbtCompound();
        data.putBoolean("raid_entity", true);
        data.putBoolean("boss_clone", true);
        data.putBoolean("battle_clone", true);
        pokemon.setPersistentData$common(data);
        pokemon.setShiny(false);
        pokemon.setScaleModifier(0.1f);

        PokemonEntity bossClone = pokemon.sendOut(raid.raidBossLocation().world(), raid.raidBossLocation().pos(), null, entity -> {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, -1, 9999, false, false));
            entity.setNoGravity(true);
            entity.setAiDisabled(true);
            entity.setMovementSpeed(0.0f);
            entity.setDrops(new DropTable());
            return Unit.INSTANCE;
        });

        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);

        if (bossClone != null) {
            raid.addClone(bossClone, player);
            pveOverride(player, bossClone, null, BattleFormat.Companion.getGEN_9_SINGLES(), false, raid.bossInfo().raidDetails().healPartyOnChallenge(), raid.raidBossLocation().borderRadius() * 2, party, raid.bossInfo().aiSkillLevel());
        }
    }

    private static BattleStartResult pveOverride(ServerPlayerEntity player, PokemonEntity pokemonEntity, @Nullable UUID leadingPokemon, BattleFormat battleFormat, boolean cloneParties, boolean healFirst, float fleeDistance, PartyStore party, int skill) {
        List<BattlePokemon> playerTeam = party.toBattleTeam(cloneParties, healFirst, leadingPokemon);
        playerTeam.sort((pokemon1, pokemon2) -> Boolean.compare(pokemon1.getHealth() <= 0, pokemon2.getHealth() <= 0));
        PlayerBattleActor playerActor = new PlayerBattleActor(player.getUuid(), playerTeam);
        PokemonBattleActor wildActor = new PokemonBattleActor(pokemonEntity.getPokemon().getUuid(), new BattlePokemon(pokemonEntity.getPokemon(), pokemonEntity.getPokemon(), pEntity -> Unit.INSTANCE), fleeDistance, new StrongBattleAI(skill));
        ErroredBattleStart errors = new ErroredBattleStart();

        if (!playerTeam.isEmpty() && playerTeam.getFirst().getHealth() <= 0) {
            errors.getParticipantErrors().get(playerActor).add(
                    BattleStartError.Companion.insufficientPokemon(
                            player,
                            battleFormat.getBattleType().getSlotsPerActor(),
                            playerActor.getPokemonList().size()
                    )
            );
        }

        if (playerActor.getPokemonList().size() < battleFormat.getBattleType().getSlotsPerActor()) {
            errors.getParticipantErrors().get(playerActor).add(
                    BattleStartError.Companion.insufficientPokemon(
                            player,
                            battleFormat.getBattleType().getSlotsPerActor(),
                            playerActor.getPokemonList().size()
                    )
            );
        }

        if (playerActor.getPokemonList().stream().anyMatch(pokemon -> {
            if (pokemon.getEntity() != null) {
                return pokemon.getEntity().isBusy();
            }
            return false;
        })) {
            errors.getParticipantErrors().get(playerActor).add(BattleStartError.Companion.targetIsBusy(player.getDisplayName() != null ? player.getDisplayName() : player.getName()));
        }

        if (BattleRegistry.getBattleByParticipatingPlayer(player) != null) {
            errors.getParticipantErrors().get(playerActor).add(BattleStartError.Companion.alreadyInBattle(playerActor));
        }

        if (pokemonEntity.getBattleId() != null) {
            errors.getParticipantErrors().get(wildActor).add(BattleStartError.Companion.alreadyInBattle(wildActor));
        }

        try {
            playerActor.setBattleTheme(pokemonEntity.getBattleTheme());
        } catch (NoSuchMethodError e) {
            NovaRaids.LOGGER.error("No Such Method Error", e);
        }

        if (errors.isEmpty()) {
            return BattleRegistry.startBattle(battleFormat, new BattleSide(playerActor), new BattleSide(wildActor), true).ifSuccessful(pokemonBattle -> {
                if (!cloneParties) {
                    pokemonEntity.setBattleId(pokemonBattle.getBattleId());
                }
                return Unit.INSTANCE;
            });
        } else {
            return errors;
        }
    }
}
