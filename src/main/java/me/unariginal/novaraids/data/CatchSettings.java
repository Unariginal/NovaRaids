package me.unariginal.novaraids.data;

import com.cobblemon.mod.common.pokemon.FormData;

public record CatchSettings(FormData form_override,
                            String features_override,
                            boolean keep_scale,
                            boolean keep_held_item,
                            boolean randomize_ivs,
                            int min_perfect_ivs,
                            boolean keep_evs,
                            boolean randomize_gender,
                            boolean randomize_nature,
                            boolean randomize_ability,
                            boolean reset_moves,
                            int level_override,
                            int shiny_chance) {}
