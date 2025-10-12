# Nova Raids Beta v0.3.4

## Bug Fixes
- Category random vouchers will no longer be a copy of category choice vouchers

## Config Changes
- Added `reduce_large_pokemon_size` boolean to config.json. With this enabled, any pokemon whose hitbox is larger than 1 block will be scaled down to 1 block.
- Added `disable_spawns_in_arena` boolean to config.json. This disables pokemon spawns in any active raid locations.
- Added `tera_type` string to bosses/your_boss.json/pokemon_details. This can be any tera type, or "random".
- Added `gmax_factor` boolean to bosses/your_boss.json/pokemon_details.
- Added `dynamax_level` integer to bosses/your_boss.json/pokemon_details. This value can be between 0 and, usually, 10. Unless you changed the maximum dynamax level in your cobblemon config.
- Added `gimmicks` list to bosses/your_boss.json/pokemon_details. 
  - Gimmick objects are formatted as follows:  
    ```json
    "gimmicks": [
      {
        "gimmick": "tera",
        "weight": 1.0
      }
    ]
    ```
  - Gimmick values can be "tera", "mega", "dynamax", or any traditional variation of those gimmick names.
  - Currently, there is no visual changes for mega or dynamax on the core boss entity, as I utilize boss clones.
  - There is no support for z-power at this time, you can set z-moves in the boss's moveset if you wish.
- Added `reroll_features_each_battle` boolean to bosses/your_boss.json/boss_details. This option will reroll the boss features from pokemon_details for each battle.
- Added `reroll_gimmick_each_battle` boolean to bosses/your_boss.json/boss_details. This option will reroll the boss gimmick from pokemon_details for each battle.
- Added `randomize_tera_type` boolean to bosses/your_boss.json/catch_settings.
- Added `reset_gmax_factor` boolean to bosses/your_boss.json/catch_settings.
- Added `dynamax_level_override` integer to bosses/your_boss.json/catch_settings.
---
# Nova Raids Beta v0.3.3
- Introduced Cobblemon 1.7 Snapshot support
---
# Nova Raids Beta v0.3.2 Hotfix
- Teleporting away from raids via /home, /spawn, etc. will no longer deal damage to the boss from the battle ending
- Added experience gain toggle to config.json, with this enabled experience will still not be gained if players manage to flee from the encounter, only from properly defeating the encounter
- Boss clone battles are properly stopped when needed
---
# Nova Raids Beta v0.3.1 Hotfix
- Teleporting away from raids via /home, /spawn, etc. will no longer deal damage to the boss from the battle ending
- Added experience gain toggle to config.json, with this enabled experience will still not be gained if players manage to flee from the encounter
- Boss clone battles are properly stopped when needed
---
# Nova Raids Beta v0.3.1 *(Patch Update)*

## Additions
- Added `/raid schedule` command with the permission node `novaraids.schedule`. This allows the user to view the upcoming raid schedules.

## Config Changes
- Most config properties will now automatically generate and fill the file with default values if they've been left out.
- Added `friendship` to boss.json/pokemon_details. (default: 50)
- Added `friendship_override` to boss.json/catch_settings. (default: 50)
- Removed `form` from boss.json/pokemon_details.
- Changed `features` in boss.json/pokemon_details to a weighted list. Previous features will automatically transfer to this new format.
  - New format:
  ```json
  "features": [
    {
      "feature": "mega_evolution=mega",
      "weight": 5.0
    }
  ]
  ```
- Added `keep_features` to boss.json/catch_settings. (default: false)
- Added `ai_skill_level` to boss.json/boss_details. This is a value from 0 to 5, 0 is random AI, 5 is the best AI. (default: 3)
- Added `blacklisted_categories` to messages.json/discord. These categories will not have webhook messages sent for them.
- Added `blacklisted_bosses` to messages.json/discord. These bosses will not have webhook messages send for them.
- Added `bosses_have_infinite_pp` to config.json/raid_settings. Setting this to true will give all bosses moves 10,000 PP.
- Added `automatic_battles` to config.json/raid_settings. Setting this to true will force players into another boss battle after a set delay.
- Added `automatic_battle_delay_seconds` to config.json/raid_settings. This is the delay before the automatic battle is started, I recommend keeping this above 2 seconds, or it may not work.
- Added `join_raid_after_voucher_use` to config.json/item_settings/voucher_settings. Setting this to true will have players automatically join the raid they start using a voucher.
- Added `player_linked_raid_balls` to config.json/item_settings/raid_ball_settings. Disabling this option will allow players to use other player's raid balls.
- Added `override_category_distribution` to boss.json/raid_details. Enabling this setting will override the full category distribution section, instead of just an individual placement.
- Renamed `override_category_rewards` in boss.json/raid_details/reward_distribution/places to `override_category_placement` to better fit its functionality.

## Bug Fixes
- Locations in other worlds will no longer make the timer count down past 0.
- Multiverse worlds now work with locations (probably?).
- NovaRaids will now load in singleplayer.
- Battles now stop properly to avoid issues such as trick item stealing, and mega evolution not reverting.
- Boss clones will now (properly) disappear when debug is false.
- Webhooks follow the config now for automatically deleting.
- If a border radius is less than 30, and Pokémon are set to be visible in config.json, they will get teleported to the border radius instead of being locked at 30.
- If a player deals damage to the boss, but loses the battle, the damage they dealt will be registered rather than it being 0.
- The `species_override` catch setting is now functional
- Players will no longer receive a catch encounter if they leave the raid
- Pokemon will no longer receive EXP from raid bosses.
- Boss battle's flee-distance is now set to the arena's border radius x 2, effectively preventing fleeing from boss battles.
- Catch encounters and rewards with the "participating" placement will now include.. participating players :)
- Certain timezones will now properly parse for schedules
---
# Nova Raids Beta v0.3.0 - The Customization Update!

If I missed anything... whoops :) I did my best

## Update Overview
This update reorganizes almost every config to allow for more customization of the mod on your server!
It also contains many bug fixes and fixes for some rare crashes.

Here's a quick list of features added in this update:
- Hiding players/Pokémon/other catch encounters during a raid.
- Global contraband, category-based contraband, and boss-based contraband
- Customizable item name, item lore, and item data for every type of item in the mod, using Kyori's MiniMessage formatting
- Global, Category-based, and Boss-based raid balls
- Location-based arena radius values
- Location-based boss rotation
- Rewards can be defined within reward pools
- Reward pools can be defined within reward distribution sections
- Bossbars are now assigned in the boss/category rather than in the bossbar config
- Categories are now folder-based instead of being defined in a single config file
- Many message changes/additions/removals
- Discord webhooks can now be dynamic, updating placeholder values at a set interval
- Bosses now have global weight and category weight for random selection
- Reward override is now reward distribution and adds on to category rewards, optionally overriding category rewards
- Catch phase settings now contain a species override
- Catch phase settings allow for placement-based catch settings for minimum perfect ivs and shiny chance
- Setting the shiny chance to 0 or below will disable it
- Added a schedule config file to define when certain raids can occur naturally
- Added gui configs to customize every gui in the mod

## Command Changes
- Added `/raid testrewards <boss> <placement> <total-players>`
- Added `/raid damage <id> <amount>` to force damage to a boss, for testing purposes
- Added `/raid world` to get the proper world ID for locations
- Added `boss`, `category`, and `global` arguments to the checkbanned command
- Added `boss`, `category`, and `global` arguments to the give pokeball command

## All Placeholders
General Placeholders:
- `%prefix%` - The prefix defined in messages.json

Boss Context Placeholders:
- `%boss%` - The ID of the boss
- `%boss.species%` - The species of the boss
- `%boss.level%` - The level of the boss
- `%boss.minimum_level%` - The minimum level required to fight the boss
- `%boss.maximum_level%` - The maximum level allowed to fight the boss
- `%boss.form%` - The form name of the boss, blank if normal
- `%boss.name%` - The display name of the boss

Raid Context Placeholders:
- `%boss.maxhp%` - The maximum HP of the boss
- `%raid.defeat_time%` - The time that the players took to defeat the boss
- `%raid.completion_time%` - The total time of the raid from start to finish
- `%raid.phase_timer%` - The current phase's timer
- `%boss.currenthp%` - The current HP of the boss
- `%raid.total_damage%` - The total damage dealt to the boss so far
- `%raid.timer%` - The total runtime of the raid
- `%raid.player_count%` - The total number of players participating in the raid
- `%raid.max_players%` - The max players allowed in a raid
- `%raid.phase%` - The current phase of the raid
- `%raid.category%` - The name of the raid's category
- `%raid.category.id%` - The id of the raid's category
- `%raid.min_players%` - The minimum players required for the raid to start
- `%raid.join_method%` - Either "A Raid Pass" or "/raid list"
- `%raid.location%` - The name of the raid location
- `%raid.location.id%` - The id of the raid location

Give Command Context Placeholders:
- `%target%` - The target player's name
- `%raid_item%` - The item given
- `%amount%` - The amount of the item
- `%source%` - The source player's name

Raid Leaderboard Context Placeholders:
- `%raid.player.place%` - The placement of the player
- `%place_suffix%` - "st" "nd" "rd" "th"
- `%raid.player%` - The name of the player
- `%raid.player.damage%` - The damage that player dealt to the boss

Contraband Context Placeholders:
- `%banned.pokemon%` - Banned Pokémon species
- `%banned.ability%` - Banned Pokémon ability
- `%banned.move%` - Banned Pokémon move
- `%banned.held_item%` - Banned Pokémon held item
- `%banned.bag_item%` - Banned item in inventory

GUI Context Placeholders:
- `%pokemon%` - Banned Pokémon species
- `%move%` - Banned move name
- `%ability%` - Banned ability name
- `%item%` - Banned item name
- `%category%` - Category-based contraband category name

## Config Changes
**Please Review The Default Config Generation To Gain Better Insight Into These Changes.**
### NovaRaids/config.json Changes
**Moved Config Items:**
- `timezone` -> `NovaRaids/schedules.json`
- `raid_settings/raid_radius` -> `NovaRaids/locations.json`
- `raid_settings/raid_pushback_radius` -> `NovaRaids/locations.json`
- `raid_settings/bosses_glow` -> Boss Configs
- `raid_settings/heal_party_on_challenge` -> Boss Configs
- `raid_settings/setup_phase_time` -> Boss Configs
- `raid_settings/fight_phase_time` -> Boss Configs
- `raid_settings/pre_catch_phase_time` -> Boss Configs
- `raid_settings/catch_phase_time` -> Boss Configs

**Config Changes:**
- Added `hide_other_catch_encounters` to the raid settings. This option will hide the other player's catch encounters during the catch phase.
- Added `hide_other_players_in_raid` to the raid settings. This option will hide other players participating in the same raid as the player.
- Added `hide_other_pokemon_in_raid` to the raid settings. This option will hide the other player's Pokémon in the same raid as the player. If this is false, Pokémon will be teleported away from the raid boss to remain out of players' way.
- Renamed `banned_section` to `global_contraband`
- Changed item settings to allow setting name and lore on all items
- Changed item settings to handle global vouchers and global voucher types (`/raid give <player> voucher <*|random>`)
- Changed item settings to handle global passes and global pass types (`/raid give <player> pass <*>`)

### NovaRaids/locations.json Changes
**Config Changes:**
- Added `name` to each location. This is the name used for the `%raid.location%` placeholder.
- Added `border_radius` to each location. This is the external border radius of the location.
- Added `boss_pushback_radius` to each location. This is the radius around the boss where players cannot enter.
- Added `boss_facing_direction` to each location. This is the direction the boss will be facing when it spawns in, in degrees.

### NovaRaids/rewards.json Changes
**Config Changes:**
- Renamed to `NovaRaids/reward_presets.json`

### NovaRaids/reward_pools.json Changes
**Config Changes:**
- Allows for defining rewards withing the reward pool, rather than requiring the reward to be defined as a preset.

### NovaRaids/bossbars.json Changes
**Config Changes:**
- The bossbars config is now a config to create predefined bossbars to be referenced in boss files and category settings files.
- Removed `phase`
- Removed `bosses` list
- Removed `categories` list
- Renamed instances of "overlay" to "actionbar"

### NovaRaids/categories.json Changes
**Config Changes:**
- Removed `NovaRaids/categories.json`
- Category settings are now specified in each Category Folder's `settings.json`
- Raid schedules are now specified in `NovaRaids/schedules.json`, this includes random raids, set time raids, and the new cron timed raids.
- Added `category_name` option, appears with the `%raid.category%` placeholder. Category id is now the folder name

### NovaRaids/messages.json Changes
**Messages Section Changes:**
- Removed `raid_list_gui_title`. Now specified in its gui config.
- Removed `raid_queue_gui_title`. Now specified in its gui config.
- Removed `contraband_gui_title`. Now specified in its gui config.
- Added `"warning_maximum_level": "%prefix% Your pokemon must be below level %boss.maximum_level%!"`
- Added `"give_command_invalid_category": "%prefix% Category %category% does not exist!"`
- Added `"give_command_invalid_pokeball": "%prefix% Pokeball %pokeball% does not exist!"`
- Added `"give_command_invalid_boss": "%prefix% Boss %boss% does not exist!"`
- Added `"give_command_failed_to_give": "%prefix% Failed to give the item!"`
- Added `"give_command_received_item": "%prefix% You received %amount% raid %raid_item%!"`
- Added `"give_command_feedback": "%prefix% Successfully gave %target% %amount% raid %raid_item%"`
- Added `"checkbanned_command_no_banned_pokemon": "%prefix% There are no banned pokemon!"`
- Added `"checkbanned_command_no_banned_moves": "%prefix% There are no banned moves!"`
- Added `"checkbanned_command_no_banned_abilities": "%prefix% There are no banned abilities!"`
- Added `"checkbanned_command_no_banned_held_items": "%prefix% There are no banned held items!"`
- Added `"checkbanned_command_no_banned_bag_items": "%prefix% There are no banned bag items!"`

**Discord Section Changes:**
- Webhook messages now dynamically update every 15 seconds.
- Webhook messages will be edited rather than sending new messages for each webhook section. New messages are sent for each raid, though.
- Added `raid_running` section, this is sent and updated during the fight phase.
- Added `raid_failed` section, this is sent if players fail to defeat the boss.
- Added `insert_leaderboard_after` option to fields. Meant for use in the `raid_running` and `raid_end` sections. This will add a dynamic leaderboard to the field directly after the field this is set in.
- Added `leaderboard_field_layout` for `raid_running` and `raid_end`. Follows the same format as other fields but allows for leaderboard placeholders.
- Added `webhook_update_rate_seconds`. This is the rate that your webhook messages will update. Increase this if it's lagging
- Added `delete_if_no_fight_phase`. This will delete the webhook post if the raid does not start due to lack of players.

### NovaRaids/bosses/your_boss.json Changes
**Config Changes:**
- Moved to `NovaRaids/bosses/category_name/bosses/your_boss.json`
- Added `boss_id`, this is the ID referenced in other configs, commands, and with the `%boss%` placeholder. Previously this was just the file name.
- Added `global_weight`. This is the weight of this boss being chosen during a global random selection process.
- Added `category_weight`. This is the weight of this boss being chosen during a category-based random selection process.
- Added `item_settings` section.
- Added `raid_details` section.

**Boss Details Section Changes:**
- Removed `display_form`
- Added `display_name`, referenced with the placeholder `%boss.name%`
- Added `apply_glowing`
- Removed `category`
- Removed `random_weight`
- Moved `body_direction` to `NovaRaids/locations.json`
- Moved `minimum_level` to the Raid Details section.
- Moved `do_catch_phase` to the Raid Details section.
- Removed `rewards_override` (moved to another section in this file, and renamed)

**(New) Item Settings Section:**
- This section contains customization options for boss-specific vouchers, passes, and raid balls.
- `allow_global_pokeballs` and `allow_category_pokeballs` to set if global raid balls and/or category raid balls can be used in this raid.

**(New) Raid Details Section:**
- `minimum_level` is moved here. This is the lowest level that a Pokémon in the player's party can be.
- Added `maximum_level`, this is the highest level that a Pokémon in the player's party can be.
- Each boss now gets their own phase timers, specified in this section.
- `heal_party_on_challenge` will heal the player's party as they challenge the boss (will not heal if the party is all fainted).
- Added a contraband section for boss-specific contraband.
- Added a bossbars section to reference a bossbar for each phase for this boss; this will override the selected boss bars in the category settings.
- Moved `rewards_override` section here, renamed to `reward_distribution`. More information in the `settings.json` changelog details.

**Catch Settings Section Changes:**
- Added `species_override`
- Shiny chance and minimum perfect ivs are now specified based on placement
- Setting shiny chance to 0 or below will disable it

### (New) NovaRaids/bosses/category_name/settings.json
**Raid Details Section:**
- This section allows for specifying details of raids within this category. Including:
  - If the raid requires a pass to join.
  - The minimum and maximum players that are allowed in a raid. 
  - Contraband for all raids in the category.
  - Bossbars for each raid in the category.

**Item Settings Section:**
- This section allows for customizing the vouchers, passes, and raid balls specific to this category.

**Reward Distribution Section:**
- This section allows for distributing reward pools to players that fall under certain placement specifications.
- Places include a specific number; "1" = first place. A percentage; "20%" = top 20% of players. Or "participating"; all participating players.
- Places allow for requiring the player to have dealt at least one damage to the boss.
- Reward pools can be defined within the distribution section, or you can use a reward pool preset defined in the reward pools config file.

### (New) NovaRaids/schedules.json
**Config Info:**
- Specify the timezone that schedules will use here from the following list:
  - `ACT`, `AET`, `AGT`, `ART`, `AST`, `BET`, `BST`, `CAT`, `CNT`, `CST`
  - `CTT`, `EAT`, `ECT`, `IET`, `IST`, `JST`, `MIT`, `NET`, `NST`
  - `PLT`, `PNT`, `PRT`, `PST`, `SST`, `VST`, `EST`, `MST`, `HST`
- Three types of schedules are usable: "random," "specific," and "cron."
- Each schedule allows for a category or boss to be selected at random based on the weight specified. If a category is selected, a boss will be randomly selected from that category.
- Random schedules allow for a minimum and maximum amount of time (in seconds) to wait before a raid will start.
- Specific schedules will start raids at specific times of each day, in the format "hh:mm:ss," based on the timezone specified.
- Cron schedules allow for very customizable repetitive starting of raids. Following the format: "`Seconds` `Minutes` `Hours` `Day Of Month` `Month` `Day Of Week`". Use this website to generate cron expressions: https://freeformatter.com/cron-expression-generator-quartz.html. Please remove the "year" section of the expression (the last one), it will crash your server. It should total six sections in total!
- These cron expressions allow for the use of #, L, W, LW, and ? modifiers as well.
- The Cron API used in Nova Raids is the following: https://github.com/jmrozanec/cron-utils
- Cron schedules also allow for the following expression nicknames:
  - @hourly
  - @daily
  - @midnight
  - @weekly
  - @monthly
  - @annually
  - @yearly

### (New) GUI Configs *(NovaRaids/guis/...)*
**Config Info**
- The configs within this folder are to modify the layout and design of the GUI and the Items within the GUI.
- This includes:
  - Three Contraband GUIs, for Global, Category, and Boss contraband
  - The `/raid list` GUI
  - Global and Category-based Raid Passes that open a GUI to select which raid to join.
  - Global and Category-based Raid Vouchers that open a GUI to select which raid to start.
  - The `/raid queue` GUI
- In the Contraband GUIs, enabling `use_hopper_gui` will disable the usage of rows. This is the default layout for contraband guis.