package me.unariginal.novaraids.data;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.JsonObject;
import net.minecraft.item.Item;

import java.util.List;

public record Contraband(JsonObject contrabandObject, List<Species> bannedPokemon, List<Move> bannedMoves, List<Ability> bannedAbilities, List<Item> bannedHeldItems, List<Item> bannedBagItems) {}
