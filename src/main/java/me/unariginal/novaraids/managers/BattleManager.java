package me.unariginal.novaraids.managers;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.EVs;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.IVs;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.CatchSettings;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

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

    public static void invoke_catch_encounter(Raid raid, ServerPlayerEntity player) {
        Pokemon pokemon = raid.raidBoss_pokemon().clone(true, null);
        NbtCompound data = new NbtCompound();
        data.putBoolean("boss_clone", true);
        data.putBoolean("catch_encounter", true);
        pokemon.setPersistentData$common(data);

        CatchSettings settings = raid.boss_info().catch_settings();

        pokemon.setShiny(new Random().nextInt(settings.shiny_chance()) == 0);

        if (!settings.keep_scale()) {
            pokemon.setScaleModifier(1.0f);
        }

        if (!settings.keep_form()) {
            pokemon.setForm(pokemon.getSpecies().getStandardForm());
        }

        if (!settings.keep_held_item()) {
            pokemon.removeHeldItem();
        }

        if (!settings.keep_evs()) {
            EVs new_evs = new EVs();
            for (Map.Entry<? extends Stat, ? extends Integer> ev : pokemon.getEvs()) {
                new_evs.add(ev.getKey(), ev.getValue());
            }
            for (Map.Entry<? extends Stat, ? extends Integer> ev : new_evs) {
                pokemon.setEV(ev.getKey(), ev.getValue());
            }
        }

        if (settings.randomize_ivs()) {
            IVs new_ivs = IVs.createRandomIVs(0);
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
        }

        if (settings.randomize_nature()) {
            pokemon.setNature(Natures.INSTANCE.getRandomNature());
        }

        if (settings.randomize_ability()) {
            pokemon.updateAbility(pokemon.getForm().getAbilities().select(pokemon.getSpecies(), pokemon.getAspects()).getFirst());
        }

        pokemon.setLevel(settings.level_override());

        PokemonEntity boss_clone = pokemon.sendOut(raid.raidBoss_location().world(), player.getPos().offset(player.getFacing(), 1), null, entity -> {
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
            BattleBuilder.INSTANCE.pve(player, boss_clone, leading_pokemon, BattleFormat.Companion.getGEN_9_SINGLES(), false, NovaRaids.INSTANCE.config().getSettings().heal_party_on_challenge(), Cobblemon.config.getDefaultFleeDistance(), party);
        }
    }

    public static void invoke_battle(Raid raid, ServerPlayerEntity player) {
        Pokemon pokemon = raid.raidBoss_pokemon_uncatchable().clone(true, null);
        NbtCompound data = new NbtCompound();
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
            BattleBuilder.INSTANCE.pve(player, boss_clone, leading_pokemon, BattleFormat.Companion.getGEN_9_SINGLES(), false, NovaRaids.INSTANCE.config().getSettings().heal_party_on_challenge(), Cobblemon.config.getDefaultFleeDistance(), party);
        }
    }
}
