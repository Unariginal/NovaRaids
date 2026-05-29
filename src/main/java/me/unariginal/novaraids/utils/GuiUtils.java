package me.unariginal.novaraids.utils;

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

public class GuiUtils {
    public static void openContrabandGui(ServerPlayerEntity player, BaseGUIItem guiItem, String contrabandType, @Nullable Category category, @Nullable Boss boss, int page) {
        Map<ItemStack, String> displayItems = new HashMap<>();
        BaseGUI gui = null;
        BaseGUIItem displayGuiItem = null;
        if (guiItem instanceof ContrabandGUIConfig.PokemonSubGUIItem pokemonSubGUIItem) {
            gui = pokemonSubGUIItem.guiSettings;
            displayGuiItem = pokemonSubGUIItem.guiSettings.pokemonDisplayItem;
            if (boss != null && category != null) {
                for (Species species : boss.raidDetails.contraband.parsedPokemon) {
                    displayItems.put(PokemonItem.from(species), TextUtils.parse(pokemonSubGUIItem.guiSettings.pokemonDisplayItem.itemName.replaceAll("%pokemon%", species.getName()).replaceAll("%category%", category.categoryName), boss));
                }
            } else if (category != null) {
                for (Species species : category.raidDetails.contraband.parsedPokemon) {
                    displayItems.put(PokemonItem.from(species), TextUtils.parse(pokemonSubGUIItem.guiSettings.pokemonDisplayItem.itemName.replaceAll("%pokemon%", species.getName()).replaceAll("%category%", category.categoryName)));
                }
            } else {
                for (Species species : CONFIG.raidSettings.globalContraband.parsedPokemon) {
                    displayItems.put(PokemonItem.from(species), TextUtils.parse(pokemonSubGUIItem.guiSettings.pokemonDisplayItem.itemName.replaceAll("%pokemon%", species.getName())));
                }
            }
        } else if (guiItem instanceof ContrabandGUIConfig.MovesSubGUIItem movesSubGUIItem) {
            gui = movesSubGUIItem.guiSettings;
            displayGuiItem = movesSubGUIItem.guiSettings.moveDisplayItem;
            if (boss != null && category != null) {
                for (MoveTemplate move : boss.raidDetails.contraband.parsedMoves) {
                    displayItems.put(movesSubGUIItem.guiSettings.moveDisplayItem.item.copy(), TextUtils.parse(movesSubGUIItem.guiSettings.moveDisplayItem.itemName.replaceAll("%move%", move.getDisplayName().getString()).replaceAll("%category%", category.categoryName), boss));
                }
            } else if (category != null) {
                for (MoveTemplate move : category.raidDetails.contraband.parsedMoves) {
                    displayItems.put(movesSubGUIItem.guiSettings.moveDisplayItem.item.copy(), TextUtils.parse(movesSubGUIItem.guiSettings.moveDisplayItem.itemName.replaceAll("%move%", move.getDisplayName().getString()).replaceAll("%category%", category.categoryName)));
                }
            } else {
                for (MoveTemplate move : CONFIG.raidSettings.globalContraband.parsedMoves) {
                    displayItems.put(movesSubGUIItem.guiSettings.moveDisplayItem.item.copy(), TextUtils.parse(movesSubGUIItem.guiSettings.moveDisplayItem.itemName.replaceAll("%move%", move.getDisplayName().getString())));
                }
            }
        } else if (guiItem instanceof ContrabandGUIConfig.AbilitiesSubGUIItem abilitiesSubGUIItem) {
            gui = abilitiesSubGUIItem.guiSettings;
            displayGuiItem = abilitiesSubGUIItem.guiSettings.abilityDisplayItem;
            if (boss != null && category != null) {
                for (AbilityTemplate ability : boss.raidDetails.contraband.parsedAbilities) {
                    displayItems.put(abilitiesSubGUIItem.guiSettings.abilityDisplayItem.item.copy(), TextUtils.parse(abilitiesSubGUIItem.guiSettings.abilityDisplayItem.itemName.replaceAll("%ability%", MiscUtilsKt.asTranslated(ability.getDisplayName()).getString()).replaceAll("%category%", category.categoryName), boss));
                }
            } else if (category != null) {
                for (AbilityTemplate ability : category.raidDetails.contraband.parsedAbilities) {
                    displayItems.put(abilitiesSubGUIItem.guiSettings.abilityDisplayItem.item.copy(), TextUtils.parse(abilitiesSubGUIItem.guiSettings.abilityDisplayItem.itemName.replaceAll("%ability%", MiscUtilsKt.asTranslated(ability.getDisplayName()).getString()).replaceAll("%category%", category.categoryName)));
                }
            } else {
                for (AbilityTemplate ability : CONFIG.raidSettings.globalContraband.parsedAbilities) {
                    displayItems.put(abilitiesSubGUIItem.guiSettings.abilityDisplayItem.item.copy(), TextUtils.parse(abilitiesSubGUIItem.guiSettings.abilityDisplayItem.itemName.replaceAll("%ability%", MiscUtilsKt.asTranslated(ability.getDisplayName()).getString())));
                }
            }
        } else if (guiItem instanceof ContrabandGUIConfig.HeldItemsSubGUIItem heldItemsSubGUIItem) {
            gui = heldItemsSubGUIItem.guiSettings;
            displayGuiItem = heldItemsSubGUIItem.guiSettings.heldItemDisplayItem;
            if (boss != null && category != null) {
                for (Item item : boss.raidDetails.contraband.parsedHeldItems) {
                    displayItems.put(item.getDefaultStack(), TextUtils.parse(heldItemsSubGUIItem.guiSettings.heldItemDisplayItem.itemName.replaceAll("%item%", item.getName().getString()).replaceAll("%category%", category.categoryName), boss));
                }
            } else if (category != null) {
                for (Item item : category.raidDetails.contraband.parsedHeldItems) {
                    displayItems.put(item.getDefaultStack(), TextUtils.parse(heldItemsSubGUIItem.guiSettings.heldItemDisplayItem.itemName.replaceAll("%item%", item.getName().getString()).replaceAll("%category%", category.categoryName)));
                }
            } else {
                for (Item item : CONFIG.raidSettings.globalContraband.parsedHeldItems) {
                    displayItems.put(item.getDefaultStack(), TextUtils.parse(heldItemsSubGUIItem.guiSettings.heldItemDisplayItem.itemName.replaceAll("%item%", item.getName().getString())));
                }
            }
        } else if (guiItem instanceof ContrabandGUIConfig.BagItemsSubGUIItem bagItemsSubGUIItem) {
            gui = bagItemsSubGUIItem.guiSettings;
            displayGuiItem = bagItemsSubGUIItem.guiSettings.bagItemDisplayItem;
            if (boss != null && category != null) {
                for (Item item : boss.raidDetails.contraband.parsedBagItems) {
                    displayItems.put(item.getDefaultStack(), TextUtils.parse(bagItemsSubGUIItem.guiSettings.bagItemDisplayItem.itemName.replaceAll("%item%", item.getName().getString()).replaceAll("%category%", category.categoryName), boss));
                }
            } else if (category != null) {
                for (Item item : category.raidDetails.contraband.parsedBagItems) {
                    displayItems.put(item.getDefaultStack(), TextUtils.parse(bagItemsSubGUIItem.guiSettings.bagItemDisplayItem.itemName.replaceAll("%item%", item.getName().getString()).replaceAll("%category%", category.categoryName)));
                }
            } else {
                for (Item item : CONFIG.raidSettings.globalContraband.parsedBagItems) {
                    displayItems.put(item.getDefaultStack(), TextUtils.parse(bagItemsSubGUIItem.guiSettings.bagItemDisplayItem.itemName.replaceAll("%item%", item.getName().getString())));
                }
            }
        }

        if (gui == null || displayGuiItem == null) {
            // TODO: Probably feedback here
            return;
        }

        Map<Integer, SimpleGui> pages = new HashMap<>();
        int pageTotal = gui.getPageTotal(displayItems.size(), displayGuiItem.symbol);
        for (int i = 1; i <= pageTotal; i++) {
            SimpleGui mainGui = new SimpleGui(gui.getScreenHandler(), player, false);
            String title = TextUtils.parse(gui.guiTitle);
            if (category != null) {
                title = title.replaceAll("%category%", category.categoryName);
            }
            if (boss != null) {
                title = TextUtils.parse(title, boss);
            }
            mainGui.setTitle(TextUtils.deserialize(title));
            pages.put(i, mainGui);
        }

        int index = 0;
        for (Map.Entry<Integer, SimpleGui> pageEntry : pages.entrySet()) {
            for (Integer slot : gui.getSlotsBySymbol(displayGuiItem.symbol)) {
                if (index < displayItems.size()) {
                    List<Text> lore = new ArrayList<>();
                    for (String line : displayGuiItem.itemLore) {
                        if (category != null) {
                            line = line.replaceAll("%category%", category.categoryName);
                        }
                        if (boss != null) {
                            line = TextUtils.parse(line, boss);
                        }
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }

                    ItemStack item = displayItems.keySet().stream().toList().get(index);
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(displayItems.values().stream().toList().get(index)))
                            .setLore(lore)
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                    index++;
                } else {
                    ItemStack item = gui.backgroundItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : gui.backgroundItem.itemLore) {
                        if (category != null) {
                            line = line.replaceAll("%category%", category.categoryName);
                        }
                        if (boss != null) {
                            line = TextUtils.parse(line, boss);
                        }
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }

                    String name = TextUtils.parse(gui.backgroundItem.itemName);
                    if (category != null) {
                        name = name.replaceAll("%category%", category.categoryName);
                    }
                    if (boss != null) {
                        name = TextUtils.parse(name, boss);
                    }

                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(name))
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
                        if (category != null) {
                            line = line.replaceAll("%category%", category.categoryName);
                        }
                        if (boss != null) {
                            line = TextUtils.parse(line, boss);
                        }
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }

                    String name = TextUtils.parse(gui.nextItem.itemName);
                    if (category != null) {
                        name = name.replaceAll("%category%", category.categoryName);
                    }
                    if (boss != null) {
                        name = TextUtils.parse(name, boss);
                    }

                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(name))
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
                        if (category != null) {
                            line = line.replaceAll("%category%", category.categoryName);
                        }
                        if (boss != null) {
                            line = TextUtils.parse(line, boss);
                        }
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }

                    String name = TextUtils.parse(gui.previousItem.itemName);
                    if (category != null) {
                        name = name.replaceAll("%category%", category.categoryName);
                    }
                    if (boss != null) {
                        name = TextUtils.parse(name, boss);
                    }

                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(name))
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
                    if (category != null) {
                        line = line.replaceAll("%category%", category.categoryName);
                    }
                    if (boss != null) {
                        line = TextUtils.parse(line, boss);
                    }
                    lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                }

                String name = TextUtils.parse(gui.closeItem.itemName);
                if (category != null) {
                    name = name.replaceAll("%category%", category.categoryName);
                }
                if (boss != null) {
                    name = TextUtils.parse(name, boss);
                }

                GuiElement element = new GuiElementBuilder(item)
                        .setName(TextUtils.deserialize(name))
                        .setLore(lore)
                        .setCallback(clickType -> pageEntry.getValue().close())
                        .build();
                pageEntry.getValue().setSlot(slot, element);
            }

            for (Integer slot : gui.getSlotsBySymbol(gui.backgroundItem.symbol)) {
                ItemStack item = gui.backgroundItem.item.copy();
                List<Text> lore = new ArrayList<>();
                for (String line : gui.backgroundItem.itemLore) {
                    if (category != null) {
                        line = line.replaceAll("%category%", category.categoryName);
                    }
                    if (boss != null) {
                        line = TextUtils.parse(line, boss);
                    }
                    lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                }

                String name = TextUtils.parse(gui.backgroundItem.itemName);
                if (category != null) {
                    name = name.replaceAll("%category%", category.categoryName);
                }
                if (boss != null) {
                    name = TextUtils.parse(name, boss);
                }

                GuiElement element = new GuiElementBuilder(item)
                        .setName(TextUtils.deserialize(name))
                        .setLore(lore)
                        .build();
                pageEntry.getValue().setSlot(slot, element);
            }
        }
        if (!pages.isEmpty()) {
            pages.get(page).open();
        } else {
            if (contrabandType.equalsIgnoreCase("pokemon")) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.checkbannedNoBannedPokemon)));
            } else if (contrabandType.equalsIgnoreCase("move")) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.checkbannedNoBannedMoves)));
            } else if (contrabandType.equalsIgnoreCase("ability")) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.checkbannedNoBannedAbilities)));
            } else if (contrabandType.equalsIgnoreCase("held_item")) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.checkbannedNoBannedHeldItems)));
            } else if (contrabandType.equalsIgnoreCase("bag_item")) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.checkbannedNoBannedBagItems)));
            }
        }
    }
}
