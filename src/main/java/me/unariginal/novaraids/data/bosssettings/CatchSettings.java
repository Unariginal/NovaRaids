package me.unariginal.novaraids.data.bosssettings;

import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;

import java.util.List;

public record CatchSettings(Species species_override,
                            int level_override,
                            FormData form_override,
                            String features_override,
                            boolean keep_scale,
                            boolean keep_held_item,
                            boolean randomize_ivs,
                            boolean keep_evs,
                            boolean randomize_gender,
                            boolean randomize_nature,
                            boolean randomize_ability,
                            boolean reset_moves,
                            List<CatchPlacement> catch_placements) {}
