package me.unariginal.novaraids.data.bosssettings;

import me.unariginal.novaraids.utils.RandomUtils;

import java.util.Map;

public record Boss(String boss_id,
                   String category_id,
                   double global_weight,
                   double category_weight,
                   PokemonDetails pokemonDetails,
                   String display_name,
                   int base_health,
                   int health_increase_per_player,
                   boolean apply_glowing,
                   Map<String, Double> spawn_locations,
                   ItemSettings item_settings,
                   RaidDetails raid_details,
                   CatchSettings catch_settings) {
    public String choose_location() {
        Map.Entry<?, Double> entry = RandomUtils.getRandomEntry(spawn_locations);
        if (entry != null) {
            return (String) entry.getKey();
        }
        return null;
    }
}
