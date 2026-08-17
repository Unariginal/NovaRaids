package me.unariginal.novaraids.placeholders.types.boss;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.placeholders.interfaces.BossPlaceholder;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.placeholders.GenericResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static me.unariginal.novaraids.placeholders.RaidGetter.getRaid;

public class BossSpecies implements BossPlaceholder {
    @Override
    public GenericResult handle(@Nullable Raid raid, @Nullable Boss boss, Boolean prioritizeRaid, List<String> args) {
        if (prioritizeRaid || boss == null) {
            if (raid == null) {
                raid = getRaid(args);
                if (raid == null) {
                    if (boss != null) {
                        String speciesName = boss.pokemonDetails.species;
                        Species species = PokemonSpecies.getByName(speciesName);
                        if (species == null) return GenericResult.invalid("null");
                        return GenericResult.valid(species.getName());
                    }
                    return GenericResult.invalid("No Active Raid");
                }
            }
            return GenericResult.valid(raid.bossPokemon.getSpecies().getName());
        }

        String speciesName = boss.pokemonDetails.species;
        Species species = PokemonSpecies.getByName(speciesName);
        if (species == null) return GenericResult.invalid("null");
        return GenericResult.valid(species.getName());
    }

    @Override
    public List<String> id() {
        return List.of("boss_species");
    }
}
