package me.unariginal.novaraids.managers;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

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
        pokemon.setPersistentData$common(data);

        Random rand = new Random();
        if (rand.nextInt((int) Cobblemon.config.getShinyRate()) == 1) {
            pokemon.setShiny(true);
        } else {
            pokemon.setShiny(false);
        }

        pokemon.setScaleModifier(1.0f);

        PokemonEntity boss_clone = pokemon.sendOut(raid.raidBoss_location().world(), player.getPos().add(1.5, 0.0, 0.0), null, entity -> {
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
            BattleBuilder.INSTANCE.pve(player, boss_clone, leading_pokemon, BattleFormat.Companion.getGEN_9_SINGLES(), false, true, Cobblemon.config.getDefaultFleeDistance(), party);
        }
    }

    public static void invoke_battle(Raid raid, ServerPlayerEntity player) {
        Pokemon pokemon = raid.raidBoss_pokemon_uncatchable().clone(true, null);
        NbtCompound data = new NbtCompound();
        data.putBoolean("boss_clone", true);
        pokemon.setPersistentData$common(data);
        pokemon.setShiny(false);

        PokemonEntity boss_clone = pokemon.sendOut(raid.raidBoss_location().world(), raid.raidBoss_location().pos(), null, entity -> {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, -1, 9999, false, false));
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
            BattleBuilder.INSTANCE.pve(player, boss_clone, leading_pokemon, BattleFormat.Companion.getGEN_9_SINGLES(), false, true, Cobblemon.config.getDefaultFleeDistance(), party);
        }
    }
}
