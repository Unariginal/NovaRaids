package me.unariginal.novaraids.guis.history;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.item.PokemonItem;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.unariginal.novaraids.config.RaidHistory;
import me.unariginal.novaraids.placeholders.ParseContext;
import me.unariginal.novaraids.utils.TextUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.unariginal.novaraids.config.ConfigManager.RAID_HISTORY;
import static me.unariginal.novaraids.config.ConfigManager.RAID_HISTORY_GUI;
import static me.unariginal.novaraids.utils.TextUtils.deserialize;

public class HistoryGUI {
    public static void openHistoryGui(ServerPlayerEntity player, String categoryId, int page) {
        List<RaidHistory> raidHistoryList = RAID_HISTORY.get(categoryId);
        if (raidHistoryList == null || raidHistoryList.isEmpty()) return;

        ParseContext.Builder parseContextBuilder = ParseContext.builder().player(player);

        Map<Integer, SimpleGui> pages = new HashMap<>();
        int pageTotal = RAID_HISTORY_GUI.getPageTotal(raidHistoryList.size(), RAID_HISTORY_GUI.raidDisplayItem.symbol);
        for (int i = 1; i <= pageTotal; i++) {
            SimpleGui gui = new SimpleGui(RAID_HISTORY_GUI.getScreenHandler(), player, false);
            gui.setTitle(deserialize(RAID_HISTORY_GUI.guiTitle, parseContextBuilder.build()));
            pages.put(i, gui);
        }

        int index = 0;
        for (Map.Entry<Integer, SimpleGui> pageEntry : pages.entrySet()) {
            for (int slot : RAID_HISTORY_GUI.getSlotsBySymbol(RAID_HISTORY_GUI.raidDisplayItem.symbol)) {
                if (index >= raidHistoryList.size()) {
                    ItemStack item = RAID_HISTORY_GUI.backgroundItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : RAID_HISTORY_GUI.backgroundItem.itemLore) {
                        lore.add(deserialize(line, parseContextBuilder.build()));
                    }

                    GuiElement guiElement = new GuiElementBuilder(item)
                            .setName(deserialize(RAID_HISTORY_GUI.backgroundItem.itemName, parseContextBuilder.build()))
                            .setLore(lore)
                            .build();
                    pageEntry.getValue().setSlot(slot, guiElement);
                } else {
                    RaidHistory raidHistory = raidHistoryList.get(index++);
                    ParseContext parseContext = parseContextBuilder.raidHistory(raidHistory).build();

                    PokemonProperties pokemonProperties = new PokemonProperties();
                    pokemonProperties.setSpecies(raidHistory.boss.species);
                    pokemonProperties.setForm(raidHistory.boss.formId);
                    pokemonProperties.setGender(raidHistory.boss.gender);
                    pokemonProperties.setShiny(raidHistory.boss.shiny);

                    ItemStack item = PokemonItem.from(pokemonProperties.create());
                    List<Text> lore = new ArrayList<>();
                    for (String line : RAID_HISTORY_GUI.raidDisplayItem.itemLore) {
                        lore.add(deserialize(line, parseContext));
                    }

                    GuiElement guiElement = GuiElementBuilder.from(item)
                            .setName(deserialize(RAID_HISTORY_GUI.raidDisplayItem.itemName, parseContext))
                            .setLore(lore)
                            .setCallback(clickType -> {
                                pageEntry.getValue().close();
                                openSomethingIdkPleaseReplaceThisMethodName(player, raidHistory);
                            })
                            .build();

                    pageEntry.getValue().setSlot(slot, guiElement);
                }
            }

            ParseContext parseContext = parseContextBuilder.build();

            if (pageEntry.getKey() < pageTotal) {
                for (Integer slot : RAID_HISTORY_GUI.getSlotsBySymbol(RAID_HISTORY_GUI.nextItem.symbol)) {
                    ItemStack item = RAID_HISTORY_GUI.nextItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : RAID_HISTORY_GUI.nextItem.itemLore) {
                        lore.add(deserialize(line, parseContext));
                    }

                    GuiElement element = new GuiElementBuilder(item)
                            .setName(deserialize(RAID_HISTORY_GUI.nextItem.itemName, parseContext))
                            .setLore(lore)
                            .setCallback(clickType -> {
                                pageEntry.getValue().close();
                                openHistoryGui(player, categoryId, pageEntry.getKey() + 1);
                            })
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                }
            }

            if (pageEntry.getKey() > 1) {
                for (Integer slot : RAID_HISTORY_GUI.getSlotsBySymbol(RAID_HISTORY_GUI.previousItem.symbol)) {
                    ItemStack item = RAID_HISTORY_GUI.previousItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : RAID_HISTORY_GUI.previousItem.itemLore) {
                        lore.add(deserialize(line, parseContext));
                    }

                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(RAID_HISTORY_GUI.previousItem.itemName, parseContext))
                            .setLore(lore)
                            .setCallback(clickType -> {
                                pageEntry.getValue().close();
                                openHistoryGui(player, categoryId, pageEntry.getKey() - 1);
                            })
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                }
            }

            for (Integer slot : RAID_HISTORY_GUI.getSlotsBySymbol(RAID_HISTORY_GUI.closeItem.symbol)) {
                ItemStack item = RAID_HISTORY_GUI.closeItem.item.copy();
                List<Text> lore = new ArrayList<>();
                for (String line : RAID_HISTORY_GUI.closeItem.itemLore) {
                    lore.add(deserialize(line, parseContext));
                }

                GuiElement guiElement = new GuiElementBuilder(item)
                        .setName(deserialize(RAID_HISTORY_GUI.closeItem.itemName, parseContext))
                        .setLore(lore)
                        .setCallback(clickType -> pageEntry.getValue().close())
                        .build();
                pageEntry.getValue().setSlot(slot, guiElement);
            }

            for (Integer slot : RAID_HISTORY_GUI.getSlotsBySymbol(RAID_HISTORY_GUI.backgroundItem.symbol)) {
                ItemStack item = RAID_HISTORY_GUI.backgroundItem.item.copy();
                List<Text> lore = new ArrayList<>();
                for (String line : RAID_HISTORY_GUI.backgroundItem.itemLore) {
                    lore.add(deserialize(line, parseContext));
                }

                GuiElement guiElement = new GuiElementBuilder(item)
                        .setName(deserialize(RAID_HISTORY_GUI.backgroundItem.itemName, parseContext))
                        .setLore(lore)
                        .build();
                pageEntry.getValue().setSlot(slot, guiElement);
            }
        }

        if (!pages.isEmpty()) pages.get(page).open();
    }

    private static void openSomethingIdkPleaseReplaceThisMethodName(ServerPlayerEntity player, RaidHistory raidHistory) {

    }
}
