package me.unariginal.novaraids.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.guis.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.ComponentChanges;

import java.io.*;
import java.util.List;

public class GuisConfig {
    private final NovaRaids nr = NovaRaids.INSTANCE;

    public RaidListGui raid_list_gui = new RaidListGui(
            "Active Raids",
            6,
            List.of(
                    "#########",
                    "#########",
                    "#########",
                    "#########",
                    "#########",
                    "P___C___N"
            ),
            new GuiButton(
                    "_",
                    "minecraft:air",
                    "",
                    List.of(),
                    ComponentChanges.EMPTY
            ),
            new GuiButton(
                    "C",
                    "minecraft:barrier",
                    "Close",
                    List.of(),
                    ComponentChanges.EMPTY
            ),
            new GuiButton(
                    "N",
                    "minecraft:arrow",
                    "Next",
                    List.of(),
                    ComponentChanges.EMPTY
            ),
            new GuiButton(
                    "P",
                    "minecraft:arrow",
                    "Previous",
                    List.of(),
                    ComponentChanges.EMPTY
            ),
            "#",
            "<light_purple>[ID: %raid.id%] %boss.name%",
            List.of(
                    "<gray>HP: %boss.currenthp%/%boss.maxhp%",
                    "<gray>Category: %raid.category%",
                    "<gray>Phase: %raid.phase%",
                    "<gray>Players: %raid.player_count%/%raid.max_players%",
                    "<gray>Raid Timer: %raid.timer%",
                    "<green>Click to join this raid!"
            ),
            List.of(
                    "<gray>HP: %boss.currenthp%/%boss.maxhp%",
                    "<gray>Category: %raid.category%",
                    "<gray>Phase: %raid.phase%",
                    "<gray>Players: %raid.player_count%/%raid.max_players%",
                    "<gray>Raid Timer: %raid.timer%",
                    "<red>This raid requires a pass!"
            ),
            List.of(
                    "<gray>HP: %boss.currenthp%/%boss.maxhp%",
                    "<gray>Category: %raid.category%",
                    "<gray>Phase: %raid.phase%",
                    "<gray>Players: %raid.player_count%/%raid.max_players%",
                    "<gray>Raid Timer: %raid.timer%",
                    "<red>This raid has already begun!"
            ),
            ComponentChanges.EMPTY
    );

    public QueueGui queue_gui = new QueueGui(
            "Queued Raids",
            6,
            List.of(
                    "#########",
                    "#########",
                    "#########",
                    "#########",
                    "#########",
                    "P___C___N"
            ),
            new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
            new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
            new GuiButton("N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
            new GuiButton("P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
            "#",
            "<light_purple>%boss.name%",
            List.of(),
            List.of("<red>Right click to cancel this raid!"),
            ComponentChanges.EMPTY
    );

    public DisplayItemGui voucher_gui = new DisplayItemGui(
            "Pick A Raid",
            6,
            List.of(
                    "#########",
                    "#########",
                    "#########",
                    "#########",
                    "#########",
                    "P___C___N"
            ),
            new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
            new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
            new GuiButton("N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
            new GuiButton("P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
            new GuiButton("#", null, "<light_purple>%boss.name%", List.of(), ComponentChanges.EMPTY)
    );

    public DisplayItemGui pass_gui = new DisplayItemGui(
            "Pick A Raid",
            6,
            List.of(
                    "#########",
                    "#########",
                    "#########",
                    "#########",
                    "#########",
                    "P___C___N"
            ),
            new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
            new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
            new GuiButton("N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
            new GuiButton("P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
            new GuiButton("#", null, "<light_purple>%boss.name%",
                    List.of(
                            "<gray>HP: %boss.currenthp%/%boss.maxhp%",
                            "<gray>Category: %raid.category%",
                            "<gray>Phase: %raid.phase%",
                            "<gray>Players: %raid.player_count%/%raid.max_players%",
                            "<gray>Raid Timer: %raid.timer%",
                            "<green>Click to join this raid!"
                    ),
                    ComponentChanges.EMPTY
            )
    );

    public ContrabandGui global_contraband_gui = new ContrabandGui(
            "Global Raid Contraband",
            0,
            List.of("PMAHB"),
            new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
            new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
            null,
            null,
            true,
            new GuiButton("P", "cobblemon:poke_ball", "<red>Banned Pokemon", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui("Global Banned Pokemon", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("#", null, "%form% %species%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton("M", "cobblemon:razor_claw", "<red>Banned Moves", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui("Global Banned Moves", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("#", "minecraft:paper", "%move%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton("A", "cobblemon:ability_patch", "<red>Banned Abilities", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui("Global Banned Abilities", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("#", "minecraft:nether_star", "%ability%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton("H", "cobblemon:leftovers", "<red>Banned Held Items", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui("Global Banned Held Items", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("#", null, "%item%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton("B", "cobblemon:potion", "<red>Banned Bag Items", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui("Global Banned Bag Items", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("#", null, "%item%", List.of(), ComponentChanges.EMPTY)
            )
    );

    public ContrabandGui category_contraband_gui = new ContrabandGui(
            "%category% Raid Contraband",
            0,
            List.of("PMAHB"),
            new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
            new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
            null,
            null,
            true,
            new GuiButton("P", "cobblemon:poke_ball", "<red>Banned Pokemon", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui("%category% Banned Pokemon", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("#", null, "%form% %species%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton("M", "cobblemon:razor_claw", "<red>Banned Moves", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui("%category% Banned Moves", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("#", "minecraft:paper", "%move%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton("A", "cobblemon:ability_patch", "<red>Banned Abilities", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui("%category% Banned Abilities", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("#", "minecraft:nether_star", "%ability%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton("H", "cobblemon:leftovers", "<red>Banned Held Items", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui("%category% Banned Held Items", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("#", null, "%item%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton("B", "cobblemon:potion", "<red>Banned Bag Items", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui("%category% Banned Bag Items", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("#", null, "%item%", List.of(), ComponentChanges.EMPTY)
            )
    );

    public ContrabandGui boss_contraband_gui = new ContrabandGui(
            "%boss.name% Raid Contraband",
            0,
            List.of("PMAHB"),
            new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
            new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
            null,
            null,
            true,
            new GuiButton("P", "cobblemon:poke_ball", "<red>Banned Pokemon", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui("%boss.name% Banned Pokemon", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("#", null, "%form% %species%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton("M", "cobblemon:razor_claw", "<red>Banned Moves", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui("%boss.name% Banned Moves", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("#", "minecraft:paper", "%move%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton("A", "cobblemon:ability_patch", "<red>Banned Abilities", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui("%boss.name% Banned Abilities", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("#", "minecraft:nether_star", "%ability%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton("H", "cobblemon:leftovers", "<red>Banned Held Items", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui("%boss.name% Banned Held Items", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("#", null, "%item%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton("B", "cobblemon:potion", "<red>Banned Bag Items", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui("%boss.name% Banned Bag Items", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton("_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton("#", null, "%item%", List.of(), ComponentChanges.EMPTY)
            )
    );

    public GuisConfig() {
        try {
            loadGuis();
        } catch (IOException | NullPointerException | UnsupportedOperationException e) {
            nr.loaded_properly = false;
            nr.logError("[RAIDS] Failed to load gui files. " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                nr.logError("  " + element.toString());
            }
        }
    }

    public void loadGuis() throws IOException, NullPointerException, UnsupportedOperationException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids").toFile();
        if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }

        File guisFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/guis").toFile();
        if (!guisFolder.exists()) {
            guisFolder.mkdirs();
        }

        File file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/guis/raid_list.json").toFile();
        if (file.createNewFile()) {
            InputStream stream = NovaRaids.class.getResourceAsStream("/raid_config_files/guis/raid_list.json");
            assert stream != null;
            OutputStream out = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = stream.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            stream.close();
            out.close();
        }
        loadRaidList(file);

        file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/guis/raid_queue.json").toFile();
        if (file.createNewFile()) {
            InputStream stream = NovaRaids.class.getResourceAsStream("/raid_config_files/guis/raid_queue.json");
            assert stream != null;
            OutputStream out = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = stream.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            stream.close();
            out.close();
        }
        loadRaidQueue(file);

        file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/guis/raid_voucher.json").toFile();
        if (file.createNewFile()) {
            InputStream stream = NovaRaids.class.getResourceAsStream("/raid_config_files/guis/raid_voucher.json");
            assert stream != null;
            OutputStream out = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = stream.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            stream.close();
            out.close();
        }
        loadRaidVoucher(file);

        file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/guis/raid_pass.json").toFile();
        if (file.createNewFile()) {
            InputStream stream = NovaRaids.class.getResourceAsStream("/raid_config_files/guis/raid_pass.json");
            assert stream != null;
            OutputStream out = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = stream.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            stream.close();
            out.close();
        }
        loadRaidPass(file);

        file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/guis/global_contraband.json").toFile();
        if (file.createNewFile()) {
            InputStream stream = NovaRaids.class.getResourceAsStream("/raid_config_files/guis/global_contraband.json");
            assert stream != null;
            OutputStream out = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = stream.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            stream.close();
            out.close();
        }
        global_contraband_gui = loadContrabandGui(file, global_contraband_gui, "guis/global_contraband");
    }

    public ContrabandGui loadContrabandGui(File file, ContrabandGui gui, String location) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonElement root = JsonParser.parseReader(new FileReader(file));
        assert root != null;
        JsonObject config = root.getAsJsonObject();

        if (ConfigHelper.checkProperty(config, "gui_title", location)) {
            gui.title = config.get("gui_title").getAsString();
        }
        if (ConfigHelper.checkProperty(config, "use_hopper_gui", location)) {
            gui.use_hopper_gui = config.get("use_hopper_gui").getAsBoolean();
        }
        if (ConfigHelper.checkProperty(config, "rows", location)) {
            gui.rows = config.get("rows").getAsInt();
        }
        if (ConfigHelper.checkProperty(config, "gui_layout", location)) {
            gui.layout = config.getAsJsonArray("gui_layout").asList().stream().map(JsonElement::getAsString).toList();
        }

        gui.banned_pokemon_button = getButton(config, "banned_pokemon", location, gui.banned_pokemon_button);
        gui.banned_pokemon = getBannedItemGui(config, "banned_pokemon", "pokemon_display_item", location, gui.banned_pokemon);
        gui.banned_moves_button = getButton(config, "banned_moves", location, gui.banned_moves_button);
        gui.banned_moves = getBannedItemGui(config, "banned_moves", "move_display_item", location, gui.banned_moves);
        gui.banned_abilities_button = getButton(config, "banned_abilities", location, gui.banned_abilities_button);
        gui.banned_abilities = getBannedItemGui(config, "banned_abilities", "ability_display_item", location, gui.banned_abilities);
        gui.banned_held_items_button = getButton(config, "banned_held_items", location, gui.banned_held_items_button);
        gui.banned_held_items = getBannedItemGui(config, "banned_held_items", "held_item_display_item", location, gui.banned_held_items);
        gui.banned_bag_items_button = getButton(config, "banned_bag_items", location, gui.banned_bag_items_button);
        gui.banned_bag_items = getBannedItemGui(config, "banned_bag_items", "bag_item_display_item", location, gui.banned_bag_items);

        return gui;
    }

    public DisplayItemGui getBannedItemGui(JsonObject config, String property, String display_item_property, String location, DisplayItemGui gui) throws NullPointerException, UnsupportedOperationException {
        if (ConfigHelper.checkProperty(config, property, location)) {
            JsonObject banned_section = config.getAsJsonObject(property);
            if (ConfigHelper.checkProperty(banned_section, "gui_settings", location)) {
                JsonObject gui_settings = banned_section.getAsJsonObject("gui_settings");
                if (ConfigHelper.checkProperty(gui_settings, "gui_title", location)) {
                    gui.title = gui_settings.get("gui_title").getAsString();
                }
                if (ConfigHelper.checkProperty(gui_settings, "rows", location)) {
                    gui.rows = gui_settings.get("rows").getAsInt();
                }
                if (ConfigHelper.checkProperty(gui_settings, "gui_layout", location)) {
                    gui.layout = gui_settings.getAsJsonArray("gui_layout").asList().stream().map(JsonElement::getAsString).toList();
                }
                gui.display_button = getButton(gui_settings, display_item_property, location, gui.display_button);
                gui.background_button = getButton(gui_settings, "background_item", location, gui.background_button);
                gui.previous_button = getButton(gui_settings, "previous_item", location, gui.previous_button);
                gui.next_button = getButton(gui_settings, "next_item", location, gui.next_button);
                gui.close_button = getButton(gui_settings, "close_item", location, gui.close_button);
            }
        }
        return gui;
    }

    public void loadRaidList(File file) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonElement root = JsonParser.parseReader(new FileReader(file));
        assert root != null;
        JsonObject config = root.getAsJsonObject();
        
        String location = "guis/raid_list";
        
        if (ConfigHelper.checkProperty(config, "gui_title", location)) {
            raid_list_gui.title = config.get("gui_title").getAsString();
        }
        if (ConfigHelper.checkProperty(config, "rows", location)) {
            raid_list_gui.rows = config.get("rows").getAsInt();
        }
        if (ConfigHelper.checkProperty(config, "gui_layout", location)) {
            raid_list_gui.layout = config.getAsJsonArray("gui_layout").asList().stream().map(JsonElement::getAsString).toList();
        }
        if (ConfigHelper.checkProperty(config, "raid_display_item", location)) {
            JsonObject raid_display_item = config.getAsJsonObject("raid_display_item");
            if (ConfigHelper.checkProperty(raid_display_item, "symbol", location)) {
                raid_list_gui.display_symbol = raid_display_item.get("symbol").getAsString();
            }
            if (ConfigHelper.checkProperty(raid_display_item, "item_name", location)) {
                raid_list_gui.display_name = raid_display_item.get("item_name").getAsString();
            }
            if (ConfigHelper.checkProperty(raid_display_item, "item_lore", location)) {
                JsonObject item_lore = raid_display_item.getAsJsonObject("item_lore");
                if (ConfigHelper.checkProperty(item_lore, "joinable", location)) {
                    raid_list_gui.joinable_lore = item_lore.getAsJsonArray("joinable").asList().stream().map(JsonElement::getAsString).toList();
                }
                if (ConfigHelper.checkProperty(item_lore, "requires_pass", location)) {
                    raid_list_gui.requires_pass_lore = item_lore.getAsJsonArray("requires_pass").asList().stream().map(JsonElement::getAsString).toList();
                }
                if (ConfigHelper.checkProperty(item_lore, "in_progress", location)) {
                    raid_list_gui.in_progress_lore = item_lore.getAsJsonArray("in_progress").asList().stream().map(JsonElement::getAsString).toList();
                }
            }
            if (ConfigHelper.checkProperty(raid_display_item, "item_data", location, false)) {
                JsonElement item_data = raid_display_item.get("item_data");
                if (item_data != null) {
                    raid_list_gui.display_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, item_data).getOrThrow().getFirst();
                }
            }
        }
        raid_list_gui.close_button = getButton(config, "close_item", location, raid_list_gui.close_button);
        raid_list_gui.next_button = getButton(config, "next_item", location, raid_list_gui.next_button);
        raid_list_gui.previous_button = getButton(config, "previous_item", location, raid_list_gui.previous_button);
        raid_list_gui.background_button = getButton(config, "background_item", location, raid_list_gui.background_button);
    }

    public void loadRaidQueue(File file) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonElement root = JsonParser.parseReader(new FileReader(file));
        assert root != null;
        JsonObject config = root.getAsJsonObject();
        
        String location = "guis/raid_queue";
        
        if (ConfigHelper.checkProperty(config, "gui_title", location)) {
            queue_gui.title = config.get("gui_title").getAsString();
        }
        if (ConfigHelper.checkProperty(config, "rows", location)) {
            queue_gui.rows = config.get("rows").getAsInt();
        }
        if (ConfigHelper.checkProperty(config, "gui_layout", location)) {
            queue_gui.layout = config.getAsJsonArray("gui_layout").asList().stream().map(JsonElement::getAsString).toList();
        }
        if (ConfigHelper.checkProperty(config, "raid_display_item", location)) {
            JsonObject raid_display_item = config.getAsJsonObject("raid_display_item");
            if (ConfigHelper.checkProperty(raid_display_item, "symbol", location)) {
                queue_gui.display_symbol = raid_display_item.get("symbol").getAsString();
            }
            if (ConfigHelper.checkProperty(raid_display_item, "item_name", location)) {
                queue_gui.display_name = raid_display_item.get("item_name").getAsString();
            }
            if (ConfigHelper.checkProperty(raid_display_item, "item_lore", location)) {
                JsonObject item_lore = raid_display_item.getAsJsonObject("item_lore");
                if (ConfigHelper.checkProperty(item_lore, "default", location)) {
                    queue_gui.default_lore = item_lore.getAsJsonArray("default").asList().stream().map(JsonElement::getAsString).toList();
                }
                if (ConfigHelper.checkProperty(item_lore, "cancelable", location)) {
                    queue_gui.cancel_lore = item_lore.getAsJsonArray("cancelable").asList().stream().map(JsonElement::getAsString).toList();
                }
            }
            if (ConfigHelper.checkProperty(raid_display_item, "item_data", location, false)) {
                JsonElement item_data = raid_display_item.get("item_data");
                if (item_data != null) {
                    queue_gui.display_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, item_data).getOrThrow().getFirst();
                }
            }
        }

        queue_gui.background_button = getButton(config, "background_item", location, queue_gui.background_button);
        queue_gui.next_button = getButton(config, "next_item", location, queue_gui.next_button);
        queue_gui.previous_button = getButton(config, "previous_item", location, queue_gui.previous_button);
        queue_gui.close_button = getButton(config, "close_item", location, queue_gui.close_button);
    }

    public void loadRaidVoucher(File file) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonElement root = JsonParser.parseReader(new FileReader(file));
        assert root != null;
        JsonObject config = root.getAsJsonObject();
        
        String location = "guis/raid_voucher";

        if (ConfigHelper.checkProperty(config, "gui_title", location)) {
            voucher_gui.title = config.get("gui_title").getAsString();
        }
        if (ConfigHelper.checkProperty(config, "rows", location)) {
            voucher_gui.rows = config.get("rows").getAsInt();
        }
        if (ConfigHelper.checkProperty(config, "gui_layout", location)) {
            voucher_gui.layout = config.getAsJsonArray("gui_layout").asList().stream().map(JsonElement::getAsString).toList();
        }
        voucher_gui.display_button = getButton(config, "raid_display_item", location, voucher_gui.display_button);
        voucher_gui.background_button = getButton(config, "background_item", location, voucher_gui.background_button);
        voucher_gui.next_button = getButton(config, "next_item", location, voucher_gui.next_button);
        voucher_gui.previous_button = getButton(config, "previous_item", location, voucher_gui.previous_button);
        voucher_gui.close_button = getButton(config, "close_item", location, voucher_gui.close_button);
    }

    public void loadRaidPass(File file) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonElement root = JsonParser.parseReader(new FileReader(file));
        assert root != null;
        JsonObject config = root.getAsJsonObject();

        String location = "guis/raid_pass";

        if (ConfigHelper.checkProperty(config, "gui_title", location)) {
            pass_gui.title = config.get("gui_title").getAsString();
        }
        if (ConfigHelper.checkProperty(config, "rows", location)) {
            pass_gui.rows = config.get("rows").getAsInt();
        }
        if (ConfigHelper.checkProperty(config, "gui_layout", location)) {
            pass_gui.layout = config.getAsJsonArray("gui_layout").asList().stream().map(JsonElement::getAsString).toList();
        }
        pass_gui.display_button = getButton(config, "raid_display_item", location, pass_gui.display_button);
        pass_gui.background_button = getButton(config, "background_item", location, voucher_gui.background_button);
        pass_gui.next_button = getButton(config, "next_item", location, voucher_gui.next_button);
        pass_gui.previous_button = getButton(config, "previous_item", location, voucher_gui.previous_button);
        pass_gui.close_button = getButton(config, "close_item", location, voucher_gui.close_button);
    }

    public GuiButton getButton(JsonObject config, String button_property, String location, GuiButton button) throws NullPointerException, UnsupportedOperationException {
        String symbol = button.symbol();
        String item = button.item();
        String item_name = button.item_name();
        List<String> item_lore = button.item_lore();
        ComponentChanges item_data = button.item_data();

        if (ConfigHelper.checkProperty(config, button_property, location)) {
            JsonObject button_item = config.getAsJsonObject(button_property);
            if (ConfigHelper.checkProperty(button_item, "symbol", location)) {
                symbol = button_item.get("symbol").getAsString();
            }
            if (ConfigHelper.checkProperty(button_item, "item", location, false)) {
                item = button_item.get("item").getAsString();
            }
            if (ConfigHelper.checkProperty(button_item, "item_name", location)) {
                item_name = button_item.get("item_name").getAsString();
            }
            if (ConfigHelper.checkProperty(button_item, "item_lore", location)) {
                item_lore = button_item.getAsJsonArray("item_lore").asList().stream().map(JsonElement::getAsString).toList();
            }
            if (ConfigHelper.checkProperty(button_item, "item_data", location, false)) {
                JsonElement item_data_element = button_item.get("item_data");
                if (item_data_element != null) {
                    item_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, item_data_element).getOrThrow().getFirst();
                }
            }
        }

        return new GuiButton(
                symbol,
                item,
                item_name,
                item_lore,
                item_data
        );
    }
}
