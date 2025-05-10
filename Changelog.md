# Nova Raids Beta v0.3.0 - The Customization Update!

## Update Overview
This update reorganizes almost every config to allow for more customization of the mod on your server!


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

### NovaRaids/messages.json Changes
**Messages Section Changes:**
- Removed `raid_list_gui_title`. Now specified in its gui config.
- Removed `raid_queue_gui_title`. Now specified in its gui config.
- Removed `contraband_gui_title`. Now specified in its gui config.
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
- Added `insert_leaderboard_after` option to fields. Meant for use in the `raid_running` and `raid_end` sections. This will add a dynamic leaderboard to the field directly after the field this is set in.
- Added `leaderboard_field_layout` for `raid_running` and `raid_end`. Follows the same format as other fields but allows for leaderboard placeholders.

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
- `minimum_level` is moved here. This is the lowest level that a pokemon in the player's party can be.
- Each boss now gets their own phase timers, specified in this section.
- `heal_party_on_challenge` will heal the player's party as they challenge the boss (will not heal if the party is all fainted).
- Added a contraband section for boss-specific contraband.
- Added a bossbars section to reference a bossbar for each phase for this boss, this will override the selected boss bars in the category settings.
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