package me.unariginal.novaraids.guis;

import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.util.MiscUtilsKt;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.unariginal.novaraids.config.guis.ContrabandGUIConfig;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.data.guis.BaseGUI;
import me.unariginal.novaraids.data.guis.BaseGUIItem;
import me.unariginal.novaraids.placeholders.ParseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.unariginal.novaraids.config.ConfigManager.CONFIG;
import static me.unariginal.novaraids.config.ConfigManager.MESSAGES;
import static me.unariginal.novaraids.utils.TextUtils.deserialize;

public class ContrabandGUI {
    public static void openContrabandGui(ServerPlayerEntity player, BaseGUIItem guiItem, String contrabandType, @Nullable Category category, @Nullable Boss boss, int page) {
        Map<ItemStack, String> displayItems = new HashMap<>();
        BaseGUI gui = null;
        BaseGUIItem displayGuiItem = null;

        if (guiItem instanceof ContrabandGUIConfig.PokemonSubGUIItem pokemonSubGUIItem) {
            gui = pokemonSubGUIItem.guiSettings;
            displayGuiItem = pokemonSubGUIItem.guiSettings.pokemonDisplayItem;
            List<Species> speciesList = CONFIG.raidSettings.globalContraband.getBannedPokemonSpecies();

            if (boss != null) speciesList = boss.raidDetails.contraband.getBannedPokemonSpecies();
            else if (category != null) speciesList = category.raidDetails.contraband.getBannedPokemonSpecies();

            for (Species species : speciesList) {
                displayItems.put(PokemonItem.from(species), pokemonSubGUIItem.guiSettings.pokemonDisplayItem.itemName.replaceAll("%pokemon%", species.getName()));
            }
        } else if (guiItem instanceof ContrabandGUIConfig.MovesSubGUIItem movesSubGUIItem) {
            gui = movesSubGUIItem.guiSettings;
            displayGuiItem = movesSubGUIItem.guiSettings.moveDisplayItem;
            List<MoveTemplate> moveTemplates = CONFIG.raidSettings.globalContraband.getBannedMoves();

            if (boss != null) moveTemplates = boss.raidDetails.contraband.getBannedMoves();
            else if (category != null) moveTemplates = category.raidDetails.contraband.getBannedMoves();

            for (MoveTemplate moveTemplate : moveTemplates) {
                displayItems.put(movesSubGUIItem.guiSettings.moveDisplayItem.item.copy(), movesSubGUIItem.guiSettings.moveDisplayItem.itemName.replaceAll("%move%", moveTemplate.getDisplayName().getString()));
            }
        } else if (guiItem instanceof ContrabandGUIConfig.AbilitiesSubGUIItem abilitiesSubGUIItem) {
            gui = abilitiesSubGUIItem.guiSettings;
            displayGuiItem = abilitiesSubGUIItem.guiSettings.abilityDisplayItem;
            List<AbilityTemplate> abilityTemplates = CONFIG.raidSettings.globalContraband.getBannedAbilities();

            if (boss != null) abilityTemplates = boss.raidDetails.contraband.getBannedAbilities();
            else if (category != null) abilityTemplates = category.raidDetails.contraband.getBannedAbilities();

            for (AbilityTemplate abilityTemplate : abilityTemplates) {
                displayItems.put(abilitiesSubGUIItem.guiSettings.abilityDisplayItem.item.copy(), abilitiesSubGUIItem.guiSettings.abilityDisplayItem.itemName.replaceAll("%ability%", MiscUtilsKt.asTranslated(abilityTemplate.getDisplayName()).getString()));
            }
        } else if (guiItem instanceof ContrabandGUIConfig.HeldItemsSubGUIItem heldItemsSubGUIItem) {
            gui = heldItemsSubGUIItem.guiSettings;
            displayGuiItem = heldItemsSubGUIItem.guiSettings.heldItemDisplayItem;
            List<Item> items = CONFIG.raidSettings.globalContraband.getBannedHeldItems();

            if (boss != null) items = boss.raidDetails.contraband.getBannedHeldItems();
            else if (category != null) items = category.raidDetails.contraband.getBannedHeldItems();

            for (Item item : items) {
                displayItems.put(item.getDefaultStack(), heldItemsSubGUIItem.guiSettings.heldItemDisplayItem.itemName.replaceAll("%item%", item.getName().getString()));
            }
        } else if (guiItem instanceof ContrabandGUIConfig.BagItemsSubGUIItem bagItemsSubGUIItem) {
            gui = bagItemsSubGUIItem.guiSettings;
            displayGuiItem = bagItemsSubGUIItem.guiSettings.bagItemDisplayItem;
            List<Item> items = CONFIG.raidSettings.globalContraband.getBannedBagItems();

            if (boss != null) items = boss.raidDetails.contraband.getBannedBagItems();
            else if (category != null) items = category.raidDetails.contraband.getBannedBagItems();

            for (Item item : items) {
                displayItems.put(item.getDefaultStack(), bagItemsSubGUIItem.guiSettings.bagItemDisplayItem.itemName.replaceAll("%item%", item.getName().getString()));
            }
        }

        if (gui == null || displayGuiItem == null) {
            // TODO: Probably feedback here
            return;
        }

        ParseContext.Builder parseContextBuilder = ParseContext.builder().player(player);
        if (category != null) parseContextBuilder.category(category);
        if (boss != null) parseContextBuilder.boss(boss).prioritizeRaid(false);
        ParseContext parseContext = parseContextBuilder.build();

        Map<Integer, SimpleGui> pages = new HashMap<>();
        int pageTotal = gui.getPageTotal(displayItems.size(), displayGuiItem.symbol);
        for (int i = 1; i <= pageTotal; i++) {
            SimpleGui mainGui = new SimpleGui(gui.getScreenHandler(), player, false);
            mainGui.setTitle(deserialize(gui.guiTitle, parseContext));
            pages.put(i, mainGui);
        }

        int index = 0;
        for (Map.Entry<Integer, SimpleGui> pageEntry : pages.entrySet()) {
            for (Integer slot : gui.getSlotsBySymbol(displayGuiItem.symbol)) {
                if (index < displayItems.size()) {
                    List<Text> lore = new ArrayList<>();
                    for (String line : displayGuiItem.itemLore) {
                        lore.add(deserialize(line, parseContext));
                    }

                    ItemStack item = displayItems.keySet().stream().toList().get(index);
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(deserialize(displayItems.values().stream().toList().get(index), parseContext))
                            .setLore(lore)
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                    index++;
                } else {
                    ItemStack item = gui.backgroundItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : gui.backgroundItem.itemLore) {
                        lore.add(deserialize(line, parseContext));
                    }

                    GuiElement element = new GuiElementBuilder(item)
                            .setName(deserialize(gui.backgroundItem.itemName, parseContext))
                            .setLore(lore)
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                }
            }

            if (pageEntry.getKey() < pageTotal) {
                for (Integer slot : gui.getSlotsBySymbol(gui.nextItem.symbol)) {
                    ItemStack item = gui.nextItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : gui.nextItem.itemLore) {
                        lore.add(deserialize(line, parseContext));
                    }

                    GuiElement element = new GuiElementBuilder(item)
                            .setName(deserialize(gui.nextItem.itemName, parseContext))
                            .setLore(lore)
                            .setCallback(clickType -> {
                                pageEntry.getValue().close();
                                openContrabandGui(player, guiItem, contrabandType, category,boss, pageEntry.getKey() + 1);
                            })
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                }
            }

            if (pageEntry.getKey() > 1) {
                for (Integer slot : gui.getSlotsBySymbol(gui.previousItem.symbol)) {
                    ItemStack item = gui.previousItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : gui.previousItem.itemLore) {
                        lore.add(deserialize(line, parseContext));
                    }

                    GuiElement element = new GuiElementBuilder(item)
                            .setName(deserialize(gui.previousItem.itemName, parseContext))
                            .setLore(lore)
                            .setCallback(clickType -> {
                                pageEntry.getValue().close();
                                openContrabandGui(player, guiItem, contrabandType, category, boss, pageEntry.getKey() - 1);
                            })
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                }
            }

            for (Integer slot : gui.getSlotsBySymbol(gui.closeItem.symbol)) {
                ItemStack item = gui.closeItem.item.copy();
                List<Text> lore = new ArrayList<>();
                for (String line : gui.closeItem.itemLore) {
                    lore.add(deserialize(line, parseContext));
                }

                GuiElement element = new GuiElementBuilder(item)
                        .setName(deserialize(gui.closeItem.itemName, parseContext))
                        .setLore(lore)
                        .setCallback(clickType -> pageEntry.getValue().close())
                        .build();
                pageEntry.getValue().setSlot(slot, element);
            }

            for (Integer slot : gui.getSlotsBySymbol(gui.backgroundItem.symbol)) {
                ItemStack item = gui.backgroundItem.item.copy();
                List<Text> lore = new ArrayList<>();
                for (String line : gui.backgroundItem.itemLore) {
                    lore.add(deserialize(line, parseContext));
                }

                GuiElement element = new GuiElementBuilder(item)
                        .setName(deserialize(gui.backgroundItem.itemName, parseContext))
                        .setLore(lore)
                        .build();
                pageEntry.getValue().setSlot(slot, element);
            }
        }

        if (!pages.isEmpty()) {
            pages.get(page).open();
        } else {
            if (contrabandType.equalsIgnoreCase("pokemon")) {
                player.sendMessage(deserialize(MESSAGES.commands.checkbannedNoBannedPokemon, parseContext));
            } else if (contrabandType.equalsIgnoreCase("move")) {
                player.sendMessage(deserialize(MESSAGES.commands.checkbannedNoBannedMoves, parseContext));
            } else if (contrabandType.equalsIgnoreCase("ability")) {
                player.sendMessage(deserialize(MESSAGES.commands.checkbannedNoBannedAbilities, parseContext));
            } else if (contrabandType.equalsIgnoreCase("held_item")) {
                player.sendMessage(deserialize(MESSAGES.commands.checkbannedNoBannedHeldItems, parseContext));
            } else if (contrabandType.equalsIgnoreCase("bag_item")) {
                player.sendMessage(deserialize(MESSAGES.commands.checkbannedNoBannedBagItems, parseContext));
            }
        }
    }
}
