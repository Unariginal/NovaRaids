package me.unariginal.novaraids.commands.command;

import com.cobblemon.mod.common.item.PokemonItem;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.data.guis.BaseGUI;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.raid.RaidManager;
import me.unariginal.novaraids.raid.RaidPhase;
import me.unariginal.novaraids.utils.TextUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.unariginal.novaraids.config.ConfigManager.MESSAGES;
import static me.unariginal.novaraids.config.ConfigManager.RAID_LIST_GUI;
import static me.unariginal.novaraids.raid.RaidManager.activeRaids;
import static net.minecraft.server.command.CommandManager.literal;

public class RaidListCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("list")
                .requires(Permissions.require("novaraids.list", 4))
                .executes(RaidListCommand::execute);
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        if (activeRaids.isEmpty()) {
            player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.noActiveRaids)));
            return 0;
        }

        Map<Integer, SimpleGui> pages = new HashMap<>();
        int pageTotal = BaseGUI.getPageTotal(activeRaids.size(), RAID_LIST_GUI.getTotalSlotsBySymbol(RAID_LIST_GUI.joinableRaidItem.symbol));
        for (int i = 1; i <= pageTotal; i++) {
            SimpleGui gui = new SimpleGui(RAID_LIST_GUI.getScreenHandler(), player, false);
            gui.setTitle(TextUtils.deserialize(TextUtils.parse(RAID_LIST_GUI.guiTitle)));
            pages.put(i, gui);
        }

        int index = 0;
        for (Map.Entry<Integer, SimpleGui> pageEntry : pages.entrySet()) {
            for (Integer slot : RAID_LIST_GUI.getSlotsBySymbol(RAID_LIST_GUI.joinableRaidItem.symbol)) {
                Raid raid = RaidManager.getRaid(index);
                if (raid != null) {
                    List<Text> lore = new ArrayList<>();

                    if (!raid.category.raidDetails.requirePass && raid.phase == RaidPhase.SETUP) {
                        for (String line : RAID_LIST_GUI.joinableRaidItem.itemLore) {
                            lore.add(TextUtils.deserialize(TextUtils.parse(line, raid)));
                        }
                    } else if (raid.phase != RaidPhase.SETUP) {
                        for (String line : RAID_LIST_GUI.inProgressRaidItem.itemLore) {
                            lore.add(TextUtils.deserialize(TextUtils.parse(line, raid)));
                        }
                    } else {
                        for (String line : RAID_LIST_GUI.passRequiredRaidItem.itemLore) {
                            lore.add(TextUtils.deserialize(TextUtils.parse(line, raid)));
                        }
                    }

                    ItemStack item = RAID_LIST_GUI.joinableRaidItem.item.copyComponentsToNewStack(PokemonItem.from(raid.bossPokemon).getItem(), 1);
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(TextUtils.parse(RAID_LIST_GUI.joinableRaidItem.itemName, raid)))
                            .setLore(lore)
                            .setCallback((num, clickType, slotActionType) -> {
                                if (clickType.isLeft) {
                                    if (raid.participatingPlayers.size() < raid.maxPlayers || Permissions.check(player, "novaraids.override") || raid.maxPlayers == -1) {
                                        if (raid.addPlayer(player.getUuid(), false)) {
                                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.joinedRaid, raid)));
                                        }
                                    } else {
                                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.maxPlayers, raid)));
                                    }
                                    pageEntry.getValue().close();
                                }
                            }).build();
                    pageEntry.getValue().setSlot(slot, element);
                    index++;
                } else {
                    ItemStack item = RAID_LIST_GUI.backgroundItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : RAID_LIST_GUI.backgroundItem.itemLore) {
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(TextUtils.parse(RAID_LIST_GUI.backgroundItem.itemName)))
                            .setLore(lore)
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                }
            }

            if (pageEntry.getKey() < pageTotal) {
                for (Integer slot : RAID_LIST_GUI.getSlotsBySymbol(RAID_LIST_GUI.nextItem.symbol)) {
                    ItemStack item = RAID_LIST_GUI.nextItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : RAID_LIST_GUI.nextItem.itemLore) {
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(TextUtils.parse(RAID_LIST_GUI.nextItem.itemName)))
                            .setLore(lore)
                            .setCallback(clickType -> {
                                pageEntry.getValue().close();
                                pages.get(pageEntry.getKey() + 1).open();
                            })
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                }
            }

            if (pageEntry.getKey() > 1) {
                for (Integer slot : RAID_LIST_GUI.getSlotsBySymbol(RAID_LIST_GUI.previousItem.symbol)) {
                    ItemStack item = RAID_LIST_GUI.previousItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : RAID_LIST_GUI.previousItem.itemLore) {
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(TextUtils.parse(RAID_LIST_GUI.previousItem.itemName)))
                            .setLore(lore)
                            .setCallback(clickType -> {
                                pageEntry.getValue().close();
                                pages.get(pageEntry.getKey() - 1).open();
                            })
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                }
            }

            for (Integer slot : RAID_LIST_GUI.getSlotsBySymbol(RAID_LIST_GUI.closeItem.symbol)) {
                ItemStack item = RAID_LIST_GUI.closeItem.item.copy();
                List<Text> lore = new ArrayList<>();
                for (String line : RAID_LIST_GUI.closeItem.itemLore) {
                    lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                }
                GuiElement element = new GuiElementBuilder(item)
                        .setName(TextUtils.deserialize(TextUtils.parse(RAID_LIST_GUI.closeItem.itemName)))
                        .setLore(lore)
                        .setCallback(clickType -> pageEntry.getValue().close())
                        .build();
                pageEntry.getValue().setSlot(slot, element);
            }

            for (Integer slot : RAID_LIST_GUI.getSlotsBySymbol(RAID_LIST_GUI.backgroundItem.symbol)) {
                ItemStack item = RAID_LIST_GUI.backgroundItem.item.copy();
                List<Text> lore = new ArrayList<>();
                for (String line : RAID_LIST_GUI.backgroundItem.itemLore) {
                    lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                }
                GuiElement element = new GuiElementBuilder(item)
                        .setName(TextUtils.deserialize(TextUtils.parse(RAID_LIST_GUI.backgroundItem.itemName)))
                        .setLore(lore)
                        .build();
                pageEntry.getValue().setSlot(slot, element);
            }
        }
        pages.get(1).open();

        return Command.SINGLE_SUCCESS;
    }
}
