package me.unariginal.novaraids.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.guis.GuiButton;
import me.unariginal.novaraids.data.guis.QueueGui;
import me.unariginal.novaraids.data.guis.RaidListGui;
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
    }

    public void loadRaidList(File file) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonElement root = JsonParser.parseReader(new FileReader(file));
        assert root != null;
        JsonObject config = root.getAsJsonObject();
        if (ConfigHelper.checkProperty(config, "gui_title", "guis/raid_list")) {
            raid_list_gui.title = config.get("gui_title").getAsString();
        }
        if (ConfigHelper.checkProperty(config, "rows", "guis/raid_list")) {
            raid_list_gui.rows = config.get("rows").getAsInt();
        }
        if (ConfigHelper.checkProperty(config, "gui_layout", "guis/raid_list")) {
            raid_list_gui.layout = config.getAsJsonArray("gui_layout").asList().stream().map(JsonElement::getAsString).toList();
        }
        if (ConfigHelper.checkProperty(config, "raid_display_item", "guis/raid_list")) {
            JsonObject raid_display_item = config.getAsJsonObject("raid_display_item");
            if (ConfigHelper.checkProperty(raid_display_item, "symbol", "guis/raid_list")) {
                raid_list_gui.display_symbol = raid_display_item.get("symbol").getAsString();
            }
            if (ConfigHelper.checkProperty(raid_display_item, "item_name", "guis/raid_list")) {
                raid_list_gui.display_name = raid_display_item.get("item_name").getAsString();
            }
            if (ConfigHelper.checkProperty(raid_display_item, "item_lore", "guis/raid_list")) {
                JsonObject item_lore = raid_display_item.getAsJsonObject("item_lore");
                if (ConfigHelper.checkProperty(item_lore, "joinable", "guis/raid_list")) {
                    raid_list_gui.joinable_lore = item_lore.getAsJsonArray("joinable").asList().stream().map(JsonElement::getAsString).toList();
                }
                if (ConfigHelper.checkProperty(item_lore, "requires_pass", "guis/raid_list")) {
                    raid_list_gui.requires_pass_lore = item_lore.getAsJsonArray("requires_pass").asList().stream().map(JsonElement::getAsString).toList();
                }
                if (ConfigHelper.checkProperty(item_lore, "in_progress", "guis/raid_list")) {
                    raid_list_gui.in_progress_lore = item_lore.getAsJsonArray("in_progress").asList().stream().map(JsonElement::getAsString).toList();
                }
            }
            if (ConfigHelper.checkProperty(raid_display_item, "item_data", "guis/raid_list", false)) {
                JsonElement item_data = raid_display_item.get("item_data");
                if (item_data != null) {
                    raid_list_gui.display_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, item_data).getOrThrow().getFirst();
                }
            }
        }
        raid_list_gui.close_button = getButton(config, "close_item", "guis/raid_list", raid_list_gui.close_button);
        raid_list_gui.next_button = getButton(config, "next_item", "guis/raid_list", raid_list_gui.next_button);
        raid_list_gui.previous_button = getButton(config, "previous_item", "guis/raid_list", raid_list_gui.previous_button);
        raid_list_gui.background_button = getButton(config, "background_item", "guis/raid_list", raid_list_gui.background_button);
    }

    public void loadRaidQueue(File file) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonElement root = JsonParser.parseReader(new FileReader(file));
        assert root != null;
        JsonObject config = root.getAsJsonObject();
        if (ConfigHelper.checkProperty(config, "gui_title", "guis/raid_queue")) {
            queue_gui.title = config.get("gui_title").getAsString();
        }
        if (ConfigHelper.checkProperty(config, "rows", "guis/raid_queue")) {
            queue_gui.rows = config.get("rows").getAsInt();
        }
        if (ConfigHelper.checkProperty(config, "gui_layout", "guis/raid_queue")) {
            queue_gui.layout = config.getAsJsonArray("gui_layout").asList().stream().map(JsonElement::getAsString).toList();
        }
        if (ConfigHelper.checkProperty(config, "raid_display_item", "guis/raid_queue")) {
            JsonObject raid_display_item = config.getAsJsonObject("raid_display_item");
            if (ConfigHelper.checkProperty(raid_display_item, "symbol", "guis/raid_queue")) {
                queue_gui.display_symbol = raid_display_item.get("symbol").getAsString();
            }
            if (ConfigHelper.checkProperty(raid_display_item, "item_name", "guis/raid_queue")) {
                queue_gui.display_name = raid_display_item.get("item_name").getAsString();
            }
            if (ConfigHelper.checkProperty(raid_display_item, "item_lore", "guis/raid_queue")) {
                JsonObject item_lore = raid_display_item.getAsJsonObject("item_lore");
                if (ConfigHelper.checkProperty(item_lore, "default", "guis/raid_queue")) {
                    queue_gui.default_lore = item_lore.getAsJsonArray("default").asList().stream().map(JsonElement::getAsString).toList();
                }
                if (ConfigHelper.checkProperty(item_lore, "cancelable", "guis/raid_queue")) {
                    queue_gui.cancel_lore = item_lore.getAsJsonArray("cancelable").asList().stream().map(JsonElement::getAsString).toList();
                }
            }
            if (ConfigHelper.checkProperty(raid_display_item, "item_data", "guis/raid_queue", false)) {
                JsonElement item_data = raid_display_item.get("item_data");
                if (item_data != null) {
                    queue_gui.display_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, item_data).getOrThrow().getFirst();
                }
            }
        }

        queue_gui.background_button = getButton(config, "background_item", "guis/raid_queue", queue_gui.background_button);
        queue_gui.next_button = getButton(config, "next_item", "guis/raid_queue", queue_gui.next_button);
        queue_gui.previous_button = getButton(config, "previous_item", "guis/raid_queue", queue_gui.previous_button);
        queue_gui.close_button = getButton(config, "close_item", "guis/raid_queue", queue_gui.close_button);
    }

    public GuiButton getButton(JsonObject config, String button_property, String location, GuiButton button) {
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
            if (ConfigHelper.checkProperty(button_item, "item", location)) {
                item = button_item.get("item").getAsString();
            }
            if (ConfigHelper.checkProperty(button_item, "item_name", location)) {
                item_name = button_item.get("item_name").getAsString();
            }
            if (ConfigHelper.checkProperty(button_item, "item_lore", location)) {
                item_lore = button_item.getAsJsonArray("item_lore").asList().stream().map(JsonElement::getAsString).toList();
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
