package me.unariginal.novaraids.config;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.guis.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.ComponentChanges;

import java.io.*;
import java.util.List;

public class GuisConfig {
    public RaidListGui raidListGui = new RaidListGui(null,
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
            new GuiButton(null,
                    "_",
                    "minecraft:air",
                    "",
                    List.of(),
                    ComponentChanges.EMPTY
            ),
            new GuiButton(null,
                    "C",
                    "minecraft:barrier",
                    "Close",
                    List.of(),
                    ComponentChanges.EMPTY
            ),
            new GuiButton(null,
                    "N",
                    "minecraft:arrow",
                    "Next",
                    List.of(),
                    ComponentChanges.EMPTY
            ),
            new GuiButton(null,
                    "P",
                    "minecraft:arrow",
                    "Previous",
                    List.of(),
                    ComponentChanges.EMPTY
            ),
            "#",
            "<light_purple>[ID: %raid.id%] %boss.id%",
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

    public QueueGui queueGui = new QueueGui(null,
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
            new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
            new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
            new GuiButton(null, "N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
            new GuiButton(null, "P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
            "#",
            "<light_purple>%boss.id%",
            List.of(),
            List.of("<red>Right click to cancel this raid!"),
            ComponentChanges.EMPTY
    );

    public DisplayItemGui voucherGui = new DisplayItemGui(
            null,
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
            new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
            new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
            new GuiButton(null, "N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
            new GuiButton(null, "P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
            new GuiButton(null, "#", null, "<light_purple>%boss.name%", List.of(), ComponentChanges.EMPTY)
    );

    public DisplayItemGui passGui = new DisplayItemGui(
            null,
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
            new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
            new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
            new GuiButton(null, "N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
            new GuiButton(null, "P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
            new GuiButton(null, "#", null, "<light_purple>%boss.name%",
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

    public ContrabandGui globalContrabandGui = new ContrabandGui(null,
            "Global Raid Contraband",
            0,
            List.of("PMAHB"),
            new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
            new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
            null,
            null,
            true,
            new GuiButton(null, "P", "cobblemon:poke_ball", "<red>Banned Pokemon", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui(
                    null,"Global Banned Pokemon", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "#", null, "%form% %species%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton(null, "M", "cobblemon:razor_claw", "<red>Banned Moves", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui(
                    null,"Global Banned Moves", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "#", "minecraft:paper", "%move%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton(null, "A", "cobblemon:ability_patch", "<red>Banned Abilities", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui(
                    null,"Global Banned Abilities", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "#", "minecraft:nether_star", "%ability%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton(null, "H", "cobblemon:leftovers", "<red>Banned Held Items", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui(
                    null,"Global Banned Held Items", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "#", null, "%item%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton(null, "B", "cobblemon:potion", "<red>Banned Bag Items", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui(
                    null,"Global Banned Bag Items", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "#", null, "%item%", List.of(), ComponentChanges.EMPTY)
            )
    );

    public ContrabandGui categoryContrabandGui = new ContrabandGui(null,
            "%category% Raid Contraband",
            0,
            List.of("PMAHB"),
            new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
            new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
            null,
            null,
            true,
            new GuiButton(null, "P", "cobblemon:poke_ball", "<red>Banned Pokemon", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui(
                    null,"%category% Banned Pokemon", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "#", null, "%form% %species%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton(null, "M", "cobblemon:razor_claw", "<red>Banned Moves", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui(
                    null,"%category% Banned Moves", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "#", "minecraft:paper", "%move%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton(null, "A", "cobblemon:ability_patch", "<red>Banned Abilities", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui(
                    null,"%category% Banned Abilities", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "#", "minecraft:nether_star", "%ability%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton(null, "H", "cobblemon:leftovers", "<red>Banned Held Items", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui(
                    null,"%category% Banned Held Items", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "#", null, "%item%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton(null, "B", "cobblemon:potion", "<red>Banned Bag Items", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui(
                    null,"%category% Banned Bag Items", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "#", null, "%item%", List.of(), ComponentChanges.EMPTY)
            )
    );

    public ContrabandGui bossContrabandGui = new ContrabandGui(null,
            "%boss.id% Raid Contraband",
            0,
            List.of("PMAHB"),
            new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
            new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
            null,
            null,
            true,
            new GuiButton(null, "P", "cobblemon:poke_ball", "<red>Banned Pokemon", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui(
                    null,"%boss.id% Banned Pokemon", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "#", null, "%form% %species%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton(null, "M", "cobblemon:razor_claw", "<red>Banned Moves", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui(
                    null,"%boss.id% Banned Moves", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "#", "minecraft:paper", "%move%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton(null, "A", "cobblemon:ability_patch", "<red>Banned Abilities", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui(
                    null,"%boss.id% Banned Abilities", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "#", "minecraft:nether_star", "%ability%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton(null, "H", "cobblemon:leftovers", "<red>Banned Held Items", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui(
                    null,"%boss.id% Banned Held Items", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "#", null, "%item%", List.of(), ComponentChanges.EMPTY)
            ),
            new GuiButton(null, "B", "cobblemon:potion", "<red>Banned Bag Items", List.of(), ComponentChanges.EMPTY),
            new DisplayItemGui(
                    null,
                    "%boss.id% Banned Bag Items", 6,
                    List.of(
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "#########",
                            "P___C___N"
                    ),
                    new GuiButton(null, "_", "minecraft:air", "", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "C", "minecraft:barrier", "Close", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "N", "minecraft:arrow", "Next", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "P", "minecraft:arrow", "Previous", List.of(), ComponentChanges.EMPTY),
                    new GuiButton(null, "#", null, "%item%", List.of(), ComponentChanges.EMPTY)
            )
    );

    public GuisConfig() {
        try {
            loadGuis();
        } catch (IOException | NullPointerException | UnsupportedOperationException e) {
            NovaRaids.LOADED = false;
            NovaRaids.LOGGER.error("[NovaRaids] Failed to load gui files.", e);
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
        loadRaidList(file);

        file.delete();
        file.createNewFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Writer writer = new FileWriter(file);
        gson.toJson(raidListGui.guiObject, writer);
        writer.close();

        file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/guis/raid_queue.json").toFile();
        loadRaidQueue(file);

        file.delete();
        file.createNewFile();
        gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        writer = new FileWriter(file);
        gson.toJson(queueGui.guiObject, writer);
        writer.close();

        file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/guis/raid_voucher.json").toFile();
        loadRaidVoucher(file);

        file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/guis/raid_pass.json").toFile();
        loadRaidPass(file);

        file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/guis/global_contraband.json").toFile();
        globalContrabandGui = loadContrabandGui(file, globalContrabandGui);

        file.delete();
        file.createNewFile();
        gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        writer = new FileWriter(file);
        gson.toJson(globalContrabandGui.guiObject, writer);
        writer.close();

        file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/guis/category_contraband.json").toFile();
        categoryContrabandGui = loadContrabandGui(file, categoryContrabandGui);

        file.delete();
        file.createNewFile();
        gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        writer = new FileWriter(file);
        gson.toJson(categoryContrabandGui.guiObject, writer);
        writer.close();

        file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/guis/boss_contraband.json").toFile();
        bossContrabandGui = loadContrabandGui(file, bossContrabandGui);

        file.delete();
        file.createNewFile();
        gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        writer = new FileWriter(file);
        gson.toJson(bossContrabandGui.guiObject, writer);
        writer.close();
    }

    public ContrabandGui loadContrabandGui(File file, ContrabandGui gui) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonObject config = new JsonObject();
        if (file.exists()) config = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();

        if (config.has("gui_title"))
            gui.title = config.get("gui_title").getAsString();
        config.remove("gui_title");
        config.addProperty("gui_title", gui.title);
        
        if (config.has("use_hopper_gui"))
            gui.useHopperGui = config.get("use_hopper_gui").getAsBoolean();
        config.remove("use_hopper_gui");
        config.addProperty("use_hopper_gui", gui.useHopperGui);
        
        if (config.has("rows"))
            gui.rows = config.get("rows").getAsInt();
        config.remove("rows");
        config.addProperty("rows", gui.rows);

        if (config.has("gui_layout"))
            gui.layout = config.getAsJsonArray("gui_layout").asList().stream().map(JsonElement::getAsString).toList();
        config.remove("gui_layout");
        JsonArray guiLayoutArray = new JsonArray();
        for (String layout : gui.layout) {
            guiLayoutArray.add(layout);
        }
        config.add("gui_layout", guiLayoutArray);

        gui.bannedPokemonButton = getButton(config, "banned_pokemon", gui.bannedPokemonButton);
        config.remove("banned_pokemon");
        config.add("banned_pokemon", gui.bannedPokemonButton.buttonObject());
        gui.bannedPokemon = getDisplayItemGui(gui.bannedPokemonButton.buttonObject(), "pokemon_display_item", gui.bannedPokemon, true);

        gui.bannedMovesButton = getButton(config, "banned_moves", gui.bannedMovesButton);
        config.remove("banned_moves");
        config.add("banned_moves", gui.bannedMovesButton.buttonObject());
        gui.bannedMoves = getDisplayItemGui(gui.bannedMovesButton.buttonObject(), "move_display_item", gui.bannedMoves, true);

        gui.bannedAbilitiesButton = getButton(config, "banned_abilities", gui.bannedAbilitiesButton);
        config.remove("banned_abilities");
        config.add("banned_abilities", gui.bannedAbilitiesButton.buttonObject());
        gui.bannedAbilities = getDisplayItemGui(gui.bannedAbilitiesButton.buttonObject(), "ability_display_item", gui.bannedAbilities, true);

        gui.bannedHeldItemsButton = getButton(config, "banned_held_items", gui.bannedHeldItemsButton);
        config.remove("banned_held_items");
        config.add("banned_held_items", gui.bannedHeldItemsButton.buttonObject());
        gui.bannedHeldItems = getDisplayItemGui(gui.bannedHeldItemsButton.buttonObject(), "held_item_display_item", gui.bannedHeldItems, true);

        gui.bannedBagItemsButton = getButton(config, "banned_bag_items", gui.bannedBagItemsButton);
        config.remove("banned_bag_items");
        config.add("banned_bag_items", gui.bannedBagItemsButton.buttonObject());
        gui.bannedBagItems = getDisplayItemGui(gui.bannedBagItemsButton.buttonObject(), "bag_item_display_item", gui.bannedBagItems, true);

        gui.backgroundButton = getButton(config, "background_item", gui.backgroundButton);
        config.remove("background_item");
        config.add("background_item", gui.backgroundButton.buttonObject());

        gui.closeButton = getButton(config, "close_item", gui.closeButton);
        config.remove("close_item");
        config.add("close_item", gui.closeButton.buttonObject());

        gui.guiObject = config;

        return gui;
    }

    public DisplayItemGui getDisplayItemGui(JsonObject config, String displayItemProperty, DisplayItemGui gui, boolean subGUI) throws NullPointerException, UnsupportedOperationException {
        if (config == null) config = new JsonObject();

        JsonObject guiSettingsObject = new JsonObject();
        if (subGUI) {
            if (config.has("gui_settings"))
                guiSettingsObject = config.getAsJsonObject("gui_settings");
        } else {
            guiSettingsObject = config;
        }

        if (guiSettingsObject.has("gui_title"))
            gui.title = guiSettingsObject.get("gui_title").getAsString();
        guiSettingsObject.remove("gui_title");
        guiSettingsObject.addProperty("gui_title", gui.title);

        if (guiSettingsObject.has("rows"))
            gui.rows = guiSettingsObject.get("rows").getAsInt();
        guiSettingsObject.remove("rows");
        guiSettingsObject.addProperty("rows", gui.rows);

        if (guiSettingsObject.has("gui_layout"))
            gui.layout = guiSettingsObject.getAsJsonArray("gui_layout").asList().stream().map(JsonElement::getAsString).toList();
        guiSettingsObject.remove("gui_layout");
        JsonArray guiLayoutArray = new JsonArray();
        for (String layout : gui.layout) {
            guiLayoutArray.add(layout);
        }
        guiSettingsObject.add("gui_layout", guiLayoutArray);

        gui.displayButton = getButton(guiSettingsObject, displayItemProperty, gui.displayButton);
        guiSettingsObject.remove(displayItemProperty);
        guiSettingsObject.add(displayItemProperty, gui.displayButton.buttonObject());

        gui.backgroundButton = getButton(guiSettingsObject, "background_item", gui.backgroundButton);
        guiSettingsObject.remove("background_item");
        guiSettingsObject.add("background_item", gui.backgroundButton.buttonObject());

        gui.previousButton = getButton(guiSettingsObject, "previous_item", gui.previousButton);
        guiSettingsObject.remove("previous_item");
        guiSettingsObject.add("previous_item", gui.previousButton.buttonObject());

        gui.nextButton = getButton(guiSettingsObject, "next_item", gui.nextButton);
        guiSettingsObject.remove("next_item");
        guiSettingsObject.add("next_item", gui.nextButton.buttonObject());

        gui.closeButton = getButton(guiSettingsObject, "close_item", gui.closeButton);
        guiSettingsObject.remove("close_item");
        guiSettingsObject.add("close_item", gui.closeButton.buttonObject());

        gui.guiObject = guiSettingsObject;

        return gui;
    }

    public void loadRaidList(File file) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonObject config = new JsonObject();
        if (file.exists()) config = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();

        if (config.has("gui_title"))
            raidListGui.title = config.get("gui_title").getAsString();
        config.remove("gui_title");
        config.addProperty("gui_title", raidListGui.title);

        if (config.has("rows"))
            raidListGui.rows = config.get("rows").getAsInt();
        config.remove("rows");
        config.addProperty("rows", raidListGui.rows);

        if (config.has("gui_layout"))
            raidListGui.layout = config.getAsJsonArray("gui_layout").asList().stream().map(JsonElement::getAsString).toList();
        config.remove("gui_layout");
        JsonArray guiLayoutArray = new JsonArray();
        for (String layout : raidListGui.layout) {
            guiLayoutArray.add(layout);
        }
        config.add("gui_layout", guiLayoutArray);

        JsonObject raidDisplayItemObject = new JsonObject();
        if (config.has("raid_display_item"))
            raidDisplayItemObject = config.getAsJsonObject("raid_display_item");

        if (raidDisplayItemObject.has("symbol"))
            raidListGui.displaySymbol = raidDisplayItemObject.get("symbol").getAsString();
        raidDisplayItemObject.remove("symbol");
        raidDisplayItemObject.addProperty("symbol", raidListGui.displaySymbol);

        if (raidDisplayItemObject.has("item_name"))
            raidListGui.displayName = raidDisplayItemObject.get("item_name").getAsString();
        raidDisplayItemObject.remove("item_name");
        raidDisplayItemObject.addProperty("item_name", raidListGui.displayName);

        JsonObject displayItemLoreObject = new JsonObject();
        if (raidDisplayItemObject.has("item_lore"))
            displayItemLoreObject = raidDisplayItemObject.get("item_lore").getAsJsonObject();

        if (displayItemLoreObject.has("joinable"))
            raidListGui.joinableLore = displayItemLoreObject.getAsJsonArray("joinable").asList().stream().map(JsonElement::getAsString).toList();
        displayItemLoreObject.remove("joinable");
        JsonArray displayItemLoreArray = new JsonArray();
        for (String line : raidListGui.joinableLore) {
            displayItemLoreArray.add(line);
        }
        displayItemLoreObject.add("joinable", displayItemLoreArray);

        if (displayItemLoreObject.has("requires_pass"))
            raidListGui.requiresPassLore = displayItemLoreObject.getAsJsonArray("requires_pass").asList().stream().map(JsonElement::getAsString).toList();
        displayItemLoreObject.remove("requires_pass");
        displayItemLoreArray = new JsonArray();
        for (String line : raidListGui.requiresPassLore) {
            displayItemLoreArray.add(line);
        }
        displayItemLoreObject.add("requires_pass", displayItemLoreArray);

        if (displayItemLoreObject.has("in_progress"))
            raidListGui.inProgressLore = displayItemLoreObject.getAsJsonArray("in_progress").asList().stream().map(JsonElement::getAsString).toList();
        displayItemLoreObject.remove("in_progress");
        displayItemLoreArray = new JsonArray();
        for (String line : raidListGui.inProgressLore) {
            displayItemLoreArray.add(line);
        }
        displayItemLoreObject.add("in_progress", displayItemLoreArray);

        raidDisplayItemObject.remove("item_lore");
        raidDisplayItemObject.add("item_lore", displayItemLoreObject);

        if (raidDisplayItemObject.has("item_data"))
            raidListGui.displayData = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, raidDisplayItemObject.get("item_data")).getOrThrow().getFirst();
        raidDisplayItemObject.remove("item_data");
        raidDisplayItemObject.add("item_data", ComponentChanges.CODEC.encode(raidListGui.displayData, JsonOps.INSTANCE, new JsonObject()).getOrThrow());

        config.remove("raid_display_item");
        config.add("raid_display_item", raidDisplayItemObject);

        raidListGui.closeButton = getButton(config, "close_item", raidListGui.closeButton);
        config.remove("close_item");
        config.add("close_item", raidListGui.closeButton.buttonObject());

        raidListGui.nextButton = getButton(config, "next_item", raidListGui.nextButton);
        config.remove("next_item");
        config.add("next_item", raidListGui.nextButton.buttonObject());

        raidListGui.previousButton = getButton(config, "previous_item", raidListGui.previousButton);
        config.remove("previous_item");
        config.add("previous_item", raidListGui.previousButton.buttonObject());

        raidListGui.backgroundButton = getButton(config, "background_item", raidListGui.backgroundButton);
        config.remove("background_item");
        config.add("background_item", raidListGui.backgroundButton.buttonObject());

        raidListGui.guiObject = config;
    }

    public void loadRaidQueue(File file) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonObject config = new JsonObject();
        if (file.exists()) config = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
        
        if (config.has("gui_title"))
            queueGui.title = config.get("gui_title").getAsString();
        config.remove("gui_title");
        config.addProperty("gui_title", queueGui.title);

        if (config.has("rows"))
            queueGui.rows = config.get("rows").getAsInt();
        config.remove("rows");
        config.addProperty("rows", queueGui.rows);

        if (config.has("gui_layout"))
            queueGui.layout = config.getAsJsonArray("gui_layout").asList().stream().map(JsonElement::getAsString).toList();
        config.remove("gui_layout");
        JsonArray layoutArray = new JsonArray();
        for (String layout : queueGui.layout) {
            layoutArray.add(layout);
        }
        config.add("gui_layout", layoutArray);

        JsonObject raidDisplayItemObject = new JsonObject();
        if (config.has("raid_display_item"))
            raidDisplayItemObject = config.getAsJsonObject("raid_display_item");

        if (raidDisplayItemObject.has("symbol"))
            queueGui.displaySymbol = raidDisplayItemObject.get("symbol").getAsString();
        raidDisplayItemObject.remove("symbol");
        raidDisplayItemObject.addProperty("symbol", queueGui.displaySymbol);

        if (raidDisplayItemObject.has("item_name"))
            queueGui.displayName = raidDisplayItemObject.get("item_name").getAsString();
        raidDisplayItemObject.remove("item_name");
        raidDisplayItemObject.addProperty("item_name", queueGui.displayName);

        JsonObject displayItemLoreObject = new JsonObject();
        if (raidDisplayItemObject.has("item_lore"))
            displayItemLoreObject = raidDisplayItemObject.getAsJsonObject("item_lore");

        if (displayItemLoreObject.has("default"))
            queueGui.defaultLore = displayItemLoreObject.getAsJsonArray("default").asList().stream().map(JsonElement::getAsString).toList();
        displayItemLoreObject.remove("default");
        JsonArray loreArray = new JsonArray();
        for (String lore : queueGui.defaultLore) {
            loreArray.add(lore);
        }
        displayItemLoreObject.add("default", loreArray);

        if (displayItemLoreObject.has("cancelable"))
            queueGui.cancelLore = displayItemLoreObject.getAsJsonArray("cancelable").asList().stream().map(JsonElement::getAsString).toList();
        displayItemLoreObject.remove("cancelable");
        loreArray = new JsonArray();
        for (String lore : queueGui.cancelLore) {
            loreArray.add(lore);
        }
        displayItemLoreObject.add("cancelable", loreArray);

        raidDisplayItemObject.remove("item_lore");
        raidDisplayItemObject.add("item_lore", displayItemLoreObject);

        if (raidDisplayItemObject.has("item_data"))
            queueGui.displayData = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, raidDisplayItemObject.get("item_data")).getOrThrow().getFirst();
        raidDisplayItemObject.remove("item_data");
        raidDisplayItemObject.add("item_data", ComponentChanges.CODEC.encode(raidListGui.displayData, JsonOps.INSTANCE, new JsonObject()).getOrThrow());

        config.remove("raid_display_item");
        config.add("raid_display_item", raidDisplayItemObject);

        queueGui.backgroundButton = getButton(config, "background_item", queueGui.backgroundButton);
        config.remove("background_item");
        config.add("background_item", queueGui.backgroundButton.buttonObject());

        queueGui.nextButton = getButton(config, "next_item", queueGui.nextButton);
        config.remove("next_item");
        config.add("next_item", queueGui.nextButton.buttonObject());

        queueGui.previousButton = getButton(config, "previous_item", queueGui.previousButton);
        config.remove("previous_item");
        config.add("previous_item", queueGui.previousButton.buttonObject());

        queueGui.closeButton = getButton(config, "close_item", queueGui.closeButton);
        config.remove("close_item");
        config.add("close_item", queueGui.closeButton.buttonObject());

        queueGui.guiObject = config;
    }

    public void loadRaidVoucher(File file) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonObject config = new JsonObject();
        if (file.exists()) config = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();

        voucherGui = getDisplayItemGui(config, "raid_display_item", voucherGui, false);

        file.delete();
        file.createNewFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Writer writer = new FileWriter(file);
        gson.toJson(voucherGui.guiObject, writer);
        writer.close();
    }

    public void loadRaidPass(File file) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonObject config = new JsonObject();
        if (file.exists()) config = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();

        passGui = getDisplayItemGui(config, "raid_display_item", passGui, false);

        file.delete();
        file.createNewFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Writer writer = new FileWriter(file);
        gson.toJson(passGui.guiObject, writer);
        writer.close();
    }

    public GuiButton getButton(JsonObject config, String buttonProperty, GuiButton button) throws NullPointerException, UnsupportedOperationException {
        String symbol = button.symbol();
        String item = button.item();
        String itemName = button.itemName();
        List<String> itemLore = button.itemLore();
        ComponentChanges itemData = button.itemData();

        JsonObject buttonObject = new JsonObject();
        if (config.has(buttonProperty))
            buttonObject = config.getAsJsonObject(buttonProperty);

        if (buttonObject.has("symbol"))
            symbol = buttonObject.get("symbol").getAsString();
        buttonObject.remove("symbol");
        buttonObject.addProperty("symbol", symbol);

        if (buttonObject.has("item"))
            item = buttonObject.get("item").getAsString();
        buttonObject.remove("item");
        buttonObject.addProperty("item", item);

        if (buttonObject.has("item_name"))
            itemName = buttonObject.get("item_name").getAsString();
        buttonObject.remove("item_name");
        buttonObject.addProperty("item_name", itemName);

        if (buttonObject.has("item_lore"))
            itemLore = buttonObject.getAsJsonArray("item_lore").asList().stream().map(JsonElement::getAsString).toList();
        buttonObject.remove("item_lore");
        JsonArray itemLoreArray = new JsonArray();
        for (String line : itemLore)
            itemLoreArray.add(line);
        buttonObject.add("item_lore", itemLoreArray);

        if (buttonObject.has("item_data"))
            itemData = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, buttonObject.get("item_data")).getOrThrow().getFirst();
        buttonObject.remove("item_data");
        buttonObject.add("item_data", ComponentChanges.CODEC.encode(itemData, JsonOps.INSTANCE, new JsonObject()).getOrThrow());

        return new GuiButton(
                buttonObject,
                symbol,
                item,
                itemName,
                itemLore,
                itemData
        );
    }
}
