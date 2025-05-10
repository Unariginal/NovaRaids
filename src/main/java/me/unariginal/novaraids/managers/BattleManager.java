package me.unariginal.novaraids.managers;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.drop.DropTable;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.ShinyChanceCalculationEvent;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
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

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class BattleManager {
    private static UUID get_leading_pokemon(PartyStore party) {
        UUID leading_pokemon = null;
        for (Pokemon party_mon : party) {
            if (!party_mon.isFainted()) {
                leading_pokemon = party_mon.getUuid();
                break;
            }
        }
        return leading_pokemon;
    }

    public static boolean checkRate(float shinyRate) {
        if (shinyRate >= 1) {
            return (kotlin.random.Random.Default.nextFloat() < (1 / shinyRate));
        } else {
            return (kotlin.random.Random.Default.nextFloat() < shinyRate);
        }
    }

    public static void invoke_catch_encounter(Raid raid, ServerPlayerEntity player, float shiny_chance, int min_perfect_ivs) {
        Pokemon pokemon = raid.boss_info().pokemonDetails().createPokemon();
        NbtCompound data = new NbtCompound();
        data.putBoolean("raid_entity", true);
        data.putBoolean("boss_clone", true);
        data.putBoolean("catch_encounter", true);
        pokemon.setPersistentData$common(data);

        CatchSettings settings = raid.boss_info().catch_settings();

        if (shiny_chance > 0) {
            AtomicReference<Float> new_shiny = new AtomicReference<>(shiny_chance);
            CobblemonEvents.SHINY_CHANCE_CALCULATION.post(new ShinyChanceCalculationEvent[]{new ShinyChanceCalculationEvent(shiny_chance, pokemon)}, event -> {
                new_shiny.set(event.calculate(player));
                return Unit.INSTANCE;
            });
            shiny_chance = new_shiny.get();
            pokemon.setShiny(checkRate(shiny_chance));
        } else {
            pokemon.setShiny(false);
        }

        if (!settings.keep_scale()) {
            pokemon.setScaleModifier(1.0f);
        }

        if (settings.form_override() != null) {
            pokemon.setForm(settings.form_override());
        }

        if (!settings.features_override().isEmpty()) {
            PokemonProperties.Companion.parse(settings.features_override()).apply(pokemon);
        }

        if (!settings.keep_held_item()) {
            pokemon.removeHeldItem();
        }

        if (settings.keep_evs()) {
            EVs new_evs = new EVs();
            for (Map.Entry<? extends Stat, ? extends Integer> ev : pokemon.getEvs()) {
                new_evs.add(ev.getKey(), ev.getValue());
            }
            for (Map.Entry<? extends Stat, ? extends Integer> ev : new_evs) {
                pokemon.setEV(ev.getKey(), ev.getValue());
            }
        } else {
            for (Map.Entry<? extends Stat, ? extends Integer> ev : pokemon.getEvs()) {
                pokemon.setEV(ev.getKey(), 0);
            }
        }

        if (settings.randomize_ivs()) {
            IVs new_ivs = IVs.createRandomIVs(min_perfect_ivs);
            for (Map.Entry<? extends Stat, ? extends Integer> iv : new_ivs) {
                pokemon.setIV(iv.getKey(), iv.getValue());
            }
        }

        if (settings.randomize_gender()) {
            pokemon.setGender(
                    (pokemon.getForm().getMaleRatio() >= 0F && pokemon.getForm().getMaleRatio() <= 1f) ?
                            Gender.GENDERLESS :
                            (pokemon.getForm().getMaleRatio() == 1F || new Random().nextFloat() < pokemon.getForm().getMaleRatio()) ?
                                    Gender.MALE : Gender.FEMALE
            );
        } else {
            pokemon.setGender(raid.raidBoss_pokemon().getGender());
        }

        if (settings.randomize_nature()) {
            pokemon.setNature(Natures.INSTANCE.getRandomNature());
        } else {
            pokemon.setNature(raid.raidBoss_pokemon().getNature());
        }

        if (settings.randomize_ability()) {
            pokemon.updateAbility(pokemon.getForm().getAbilities().select(pokemon.getSpecies(), pokemon.getAspects()).getFirst());
        } else {
            pokemon.updateAbility(raid.raidBoss_pokemon().getAbility());
        }

        if (settings.reset_moves()) {
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

        pokemon.setLevel(settings.level_override());

        PokemonEntity boss_clone = pokemon.sendOut(raid.raidBoss_location().world(), player.getPos().offset(player.getFacing(), 1), null, entity -> {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, -1, 9999, false, false));
            entity.setNoGravity(true);
            entity.setMovementSpeed(0.0f);
            entity.setDrops(new DropTable());
            return Unit.INSTANCE;
        });

        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        UUID leading_pokemon = get_leading_pokemon(party);

        if (boss_clone != null && leading_pokemon != null) {
            raid.add_clone(boss_clone, player);
            BattleBuilder.INSTANCE.pve(player, boss_clone, leading_pokemon, BattleFormat.Companion.getGEN_9_SINGLES(), false, raid.boss_info().raid_details().heal_party_on_challenge(), Cobblemon.config.getDefaultFleeDistance(), party);
        }
    }

    public static void invoke_battle(Raid raid, ServerPlayerEntity player) {
        Pokemon pokemon = raid.boss_info().pokemonDetails().createPokemon();
        pokemon.getCustomProperties().add(UncatchableProperty.INSTANCE.uncatchable());
        pokemon.setAbility$common(raid.raidBoss_pokemon_uncatchable().getAbility());
        pokemon.setGender(raid.raidBoss_pokemon_uncatchable().getGender());
        pokemon.setNature(raid.raidBoss_pokemon_uncatchable().getNature());
        NbtCompound data = new NbtCompound();
        data.putBoolean("raid_entity", true);
        data.putBoolean("boss_clone", true);
        data.putBoolean("catch_encounter", false);
        pokemon.setPersistentData$common(data);
        pokemon.setShiny(false);
        pokemon.setScaleModifier(0.1f);

        PokemonEntity boss_clone = pokemon.sendOut(raid.raidBoss_location().world(), raid.raidBoss_location().pos(), null, entity -> {
            if (!NovaRaids.INSTANCE.debug) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, -1, 9999, false, false));
            }
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, -1, 9999, false, false));
            entity.setNoGravity(true);
            entity.setAiDisabled(true);
            entity.setMovementSpeed(0.0f);
            entity.setDrops(null);
            return Unit.INSTANCE;
        });

        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        UUID leading_pokemon = get_leading_pokemon(party);

        if (boss_clone != null && leading_pokemon != null) {
            raid.add_clone(boss_clone, player);
            BattleBuilder.INSTANCE.pve(player, boss_clone, leading_pokemon, BattleFormat.Companion.getGEN_9_SINGLES(), false, raid.boss_info().raid_details().heal_party_on_challenge(), Cobblemon.config.getDefaultFleeDistance(), party);
        }
    }
}
