| PlaceholderAPI                              | Default                | Context                      | Description                                                                                      |
|---------------------------------------------|------------------------|------------------------------|--------------------------------------------------------------------------------------------------|
| %novaraids:prefix%                          | %prefix%               | Global                       | Prefix defined in `messages.json`                                                                |
| %novaraids:raid_id%                         | %raid.id%              | Active Raid                  | ID of this raid used for various commands such as `/raid join` or `/raid skipphase`              |
| %novaraids:raid_uuid%                       | %raid.uuid%            | Active Raid                  | Unique ID of the raid                                                                            |
| %novaraids:raid_total_damage%               | %raid.total_damage%    | Active Raid                  | Total damage done to the boss so far                                                             |
| %novaraids:raid_timer%                      | %raid.timer%           | Active Raid                  | Time since the raid started                                                                      |
| %novaraids:raid_phase_timer%                | %raid.phase_timer%     | Active Raid                  | Time since the current raid phase started                                                        |
| %novaraids:raid_phase%                      | %raid.phase%           | Active Raid                  | Current phase of the raid                                                                        |
| %novaraids:raid_player_count%               | %raid.player_count%    | Active Raid                  | Total participating players in this raid                                                         |
| %novaraids:raid_modifier_id%                | %raid.modifier.id%     | Active Raid                  | Modifier ID or empty string                                                                      |
| %novaraids:raid_modifier%                   | %raid.modifier%        | Active Raid                  | Modifier Name or empty string                                                                    |
| %novaraids:raid_min_players%                | %raid.min_players%     | Active Raid                  | Minimum players required to start the fight phase                                                |
| %novaraids:raid_min_party_size%             | %raid.min_party_size%  | Active Raid                  | Minimum party member count required to join this raid                                            |
| %novaraids:raid_min_level%                  | %raid.min_level%       | Active Raid                  | Minimum pokemon level required to join this raid                                                 |
| %novaraids:raid_max_players%                | %raid.max_players%     | Active Raid                  | Maximum players allowed in this raid, or ∞ if -1                                                 |
| %novaraids:raid_max_party_size%             | %raid.max_party_size%  | Active Raid                  | Maximum party member count allowed when joining this raid                                        |
| %novaraids:raid_max_level%                  | %raid.max_level%       | Active Raid                  | Maximum pokemon level allowed when joining this raid                                             |
| %novaraids:raid_max_health%                 | %raid.maxhp%           | Active Raid                  | Maximum health of this raid boss                                                                 |
| %novaraids:raid_location_id%                | %raid.location.id%     | Active Raid                  | Location ID of this raid                                                                         |
| %novaraids:raid_location%                   | %raid.location%        | Active Raid                  | Location name of this raid                                                                       |
| %novaraids:raid_join_method%                | %raid.join_method%     | Active Raid                  | "A Raid Pass" or "/raid list" depending on if a pass is required                                 |
| %novaraids:raid_health%                     | %raid.currenthp%       | Active Raid                  | Current health of this raid boss                                                                 |
| %novaraids:raid_category_id%                | %raid.category.id%     | Active Raid                  | Category ID of this raid                                                                         |
| %novaraids:raid_category%                   | %raid.category%        | Active Raid                  | Category Name of this raid                                                                       |
| %novaraids:raid_defeated_time%              | %raid.defeat_time%     | Active Raid Post-Fight Phase | Time taken to defeat the boss                                                                    |
| %novaraids:raid_completion_time%            | %raid.completion_time% | Active Raid Ending           | Time taken to complete the raid from setup to win/loss                                           |
| %novaraids:boss_name%                       | %boss.name%            | Boss                         | Boss display name                                                                                |
| %novaraids:boss_ability%                    |                        | Boss                         | Boss ability*                                                                                    |
| %novaraids:boss_dmax_level%                 |                        | Boss                         | Boss dynamax level*                                                                              |
| %novaraids:boss_evs [stat]%                 |                        | Boss                         | Boss ev of provided stat key*                                                                    |
| %novaraids:boss_form%                       |                        | Boss                         | Boss form name*                                                                                  |
| %novaraids:boss_friendship%                 |                        | Boss                         | Boss friendship*                                                                                 |
| %novaraids:boss_gender%                     |                        | Boss                         | Boss gender*                                                                                     |
| %novaraids:boss_gmax_factor%                |                        | Boss                         | Boss gmax factor*                                                                                |
| %novaraids:boss_held_item%                  |                        | Boss                         | Boss held item                                                                                   |
| %novaraids:boss_ivs [stat]%                 |                        | Boss                         | Boss iv of provided stat key*                                                                    |
| %novaraids:boss_level%                      |                        | Boss                         | Boss level*                                                                                      |
| %novaraids:boss_moves [slot]%               |                        | Boss                         | Boss move in the provided move slot                                                              |
| %novaraids:boss_nature%                     |                        | Boss                         | Boss nature*                                                                                     |
| %novaraids:boss_scale%                      |                        | Boss                         | Boss scale*                                                                                      |
| %novaraids:boss_shiny%                      |                        | Boss                         | Is the boss shiny?*                                                                              |
| %novaraids:boss_species%                    |                        | Boss                         | Boss species                                                                                     |
| %novaraids:boss_tera_type%                  |                        | Boss                         | Boss tera type                                                                                   |
| %novaraids:modifier_id%                     |                        | Category Modifier            | ID of the category modifier                                                                      |
| %novaraids:modifier_name%                   |                        | Category Modifier            | Name of the category modifier                                                                    |
| %novaraids:modifier_weight%                 |                        | Category Modifier            | Weight of the category modifier                                                                  |
| %novaraids:modifier_chance%                 |                        | Category Modifier            | Percentage chance of this category modifier being selected from all available in it's category   |
| %novaraids:modifier_catch_level_offset%     |                        | Category Modifier            | Catch encounter level offset with this category modifier                                         |
| %novaraids:modifier_dmax_level_offset%      |                        | Category Modifier            | Boss dynamax level offset with this category modifier                                            |
| %novaraids:modifier_evs [stat]%             |                        | Category Modifier            | Boss ev offset for the provided stat key with this category modifier                             |
| %novaraids:modifier_health_increase_offset% |                        | Category Modifier            | Boss health increase offset with this category                                                   |
| %novaraids:modifier_health_offset%          |                        | Category Modifier            | Boss base health offset with this category modifier                                              |
| %novaraids:modifier_ivs [stat]%             |                        | Category Modifier            | Boss iv offset for the provided stat key with this category modifier                             |
| %novaraids:modifier_level_offset%           |                        | Category Modifier            | Boss level offset with this category modifier                                                    |
| %novaraids:modifier_max_level_offset%       |                        | Category Modifier            | Offset of the max allowed party level with this category modifier                                |
| %novaraids:modifier_max_party_size_offset%  |                        | Category Modifier            | Offset of the max allowed party size with this category modifier                                 |
| %novaraids:modifier_min_level_offset%       |                        | Category Modifier            | Offset of the min allowed party level with this category modifier                                |
| %novaraids:modifier_min_party_size_offset%  |                        | Category Modifier            | Offset of the min allowed party size with this category modifier                                 |
| %novaraids:modifier_scale_offset%           |                        | Category Modifier            | Boss scale modifier offset with this category modifier                                           |
| %novaraids:modifier_shiny_override%         |                        | Category Modifier            | Boss shiny override with this category modifier                                                  |
| %novaraids:modifier_skill_level_offset%     |                        | Category Modifier            | AI skill level offset with this category modifier                                                |

\* - Uses raid boss entity if an active raid exists or is provided. Otherwise, pulls from the boss file and rolls a random aspect if necessary.
