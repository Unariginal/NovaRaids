package me.unariginal.novaraids.commands.command;

import com.cobblemon.mod.common.item.PokemonItem;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.data.guis.BaseGUI;
import me.unariginal.novaraids.placeholders.ParseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.unariginal.novaraids.config.ConfigManager.MESSAGES;
import static me.unariginal.novaraids.config.ConfigManager.RAID_QUEUE_GUI;
import static me.unariginal.novaraids.raid.RaidManager.queuedRaids;
import static me.unariginal.novaraids.utils.TextUtils.deserialize;
import static net.minecraft.server.command.CommandManager.literal;

public class RaidQueueCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("queue")
                .requires(Permissions.require("novaraids.queue", 4))
                .executes(RaidQueueCommand::execute);
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        if (queuedRaids.isEmpty()) player.sendMessage(deserialize(MESSAGES.feedback.noQueuedRaids, ParseContext.builder().player(player).build()));

        openGuiPage(player, 1);
        return Command.SINGLE_SUCCESS;
    }

    private static void openGuiPage(ServerPlayerEntity player, int page) {
        ParseContext.Builder parseContextBuilder = ParseContext.builder().player(player);

        Map<Integer, SimpleGui> pages = new HashMap<>();
        int pageTotal = BaseGUI.getPageTotal(queuedRaids.size(), RAID_QUEUE_GUI.getTotalSlotsBySymbol(RAID_QUEUE_GUI.raidItem.symbol));
        for (int i = 1; i <= pageTotal; i++) {
            SimpleGui gui = new SimpleGui(RAID_QUEUE_GUI.getScreenHandler(), player, false);
            gui.setTitle(deserialize(RAID_QUEUE_GUI.guiTitle, parseContextBuilder.build()));
            pages.put(i, gui);
        }

        int index = 0;
        for (Map.Entry<Integer, SimpleGui> pageEntry : pages.entrySet()) {
            for (Integer slot : RAID_QUEUE_GUI.getSlotsBySymbol(RAID_QUEUE_GUI.raidItem.symbol)) {
                if (index < queuedRaids.size()) {
                    Boss boss = queuedRaids.stream().toList().get(index).boss;
                    ParseContext parseContext = parseContextBuilder.boss(boss).prioritizeRaid(false).build();

                    List<Text> lore = new ArrayList<>();
                    if (Permissions.check(player, "novaraids.cancelqueue", 4)) {
                        for (String line : RAID_QUEUE_GUI.cancelableRaidItem.itemLore) {
                            lore.add(deserialize(line, parseContext));
                        }
                    } else {
                        for (String line : RAID_QUEUE_GUI.raidItem.itemLore) {
                            lore.add(deserialize(line, parseContext));
                        }
                    }

                    ItemStack item = PokemonItem.from(boss.pokemonDetails.createDisplayPokemon());
                    int finalIndex = index;
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(deserialize(RAID_QUEUE_GUI.raidItem.itemName, parseContext))
                            .setLore(lore)
                            .setCallback((num, clickType, slotActionType) -> {
                                if (clickType.isRight) {
                                    if (Permissions.check(player, "novaraids.cancelqueue", 4)) {
                                        pageEntry.getValue().close();
                                        queuedRaids.stream().toList().get(finalIndex).cancelItem();
                                        player.sendMessage(deserialize(MESSAGES.feedback.queueItemCancelled, parseContext));
                                        queuedRaids.remove(queuedRaids.stream().toList().get(finalIndex));
                                        openGuiPage(player, pageEntry.getKey());
                                    }
                                }
                            }).build();
                    pageEntry.getValue().setSlot(slot, element);
                    index++;
                } else {
                    ParseContext parseContext = parseContextBuilder.build();

                    ItemStack item = RAID_QUEUE_GUI.backgroundItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : RAID_QUEUE_GUI.backgroundItem.itemLore) {
                        lore.add(deserialize(line, parseContext));
                    }
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(deserialize(RAID_QUEUE_GUI.backgroundItem.itemName, parseContext))
                            .setLore(lore)
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                }
            }
            ParseContext parseContext = parseContextBuilder.build();

            if (pageEntry.getKey() < pageTotal) {
                for (Integer slot : RAID_QUEUE_GUI.getSlotsBySymbol(RAID_QUEUE_GUI.nextItem.symbol)) {
                    ItemStack item = RAID_QUEUE_GUI.nextItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : RAID_QUEUE_GUI.nextItem.itemLore) {
                        lore.add(deserialize(line, parseContext));
                    }
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(deserialize(RAID_QUEUE_GUI.nextItem.itemName, parseContext))
                            .setLore(lore)
                            .setCallback(clickType -> {
                                pageEntry.getValue().close();
                                openGuiPage(player, pageEntry.getKey() + 1);
                            })
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                }
            }

            if (pageEntry.getKey() > 1) {
                for (Integer slot : RAID_QUEUE_GUI.getSlotsBySymbol(RAID_QUEUE_GUI.previousItem.symbol)) {
                    ItemStack item = RAID_QUEUE_GUI.previousItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : RAID_QUEUE_GUI.previousItem.itemLore) {
                        lore.add(deserialize(line, parseContext));
                    }
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(deserialize(RAID_QUEUE_GUI.previousItem.itemName, parseContext))
                            .setLore(lore)
                            .setCallback(clickType -> {
                                pageEntry.getValue().close();
                                openGuiPage(player, pageEntry.getKey() - 1);
                            })
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                }
            }

            for (Integer slot : RAID_QUEUE_GUI.getSlotsBySymbol(RAID_QUEUE_GUI.closeItem.symbol)) {
                ItemStack item = RAID_QUEUE_GUI.closeItem.item.copy();
                List<Text> lore = new ArrayList<>();
                for (String line : RAID_QUEUE_GUI.closeItem.itemLore) {
                    lore.add(deserialize(line, parseContext));
                }
                GuiElement element = new GuiElementBuilder(item)
                        .setName(deserialize(RAID_QUEUE_GUI.closeItem.itemName, parseContext))
                        .setLore(lore)
                        .setCallback(clickType -> pageEntry.getValue().close())
                        .build();
                pageEntry.getValue().setSlot(slot, element);
            }

            for (Integer slot : RAID_QUEUE_GUI.getSlotsBySymbol(RAID_QUEUE_GUI.backgroundItem.symbol)) {
                ItemStack item = RAID_QUEUE_GUI.backgroundItem.item.copy();
                List<Text> lore = new ArrayList<>();
                for (String line : RAID_QUEUE_GUI.backgroundItem.itemLore) {
                    lore.add(deserialize(line, parseContext));
                }
                GuiElement element = new GuiElementBuilder(item)
                        .setName(deserialize(RAID_QUEUE_GUI.backgroundItem.itemName, parseContext))
                        .setLore(lore)
                        .build();
                pageEntry.getValue().setSlot(slot, element);
            }
        }

        if (pages.containsKey(page)) pages.get(page).open();
        else {
            // TODO: Feedback
        }
    }
}
