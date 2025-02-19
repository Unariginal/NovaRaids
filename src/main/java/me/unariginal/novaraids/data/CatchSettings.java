package me.unariginal.novaraids.data;

public record CatchSettings(boolean keep_form,
                            boolean keep_scale,
                            boolean keep_held_item,
                            boolean randomize_ivs,
                            boolean keep_evs,
                            boolean randomize_gender,
                            boolean randomize_nature,
                            boolean randomize_ability,
                            int level_override,
                            int shiny_chance) {}
