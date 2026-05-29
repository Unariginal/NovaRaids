package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.unariginal.novaraids.config.guis.ContrabandGUIConfig;
import me.unariginal.novaraids.utils.TextUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

import static me.unariginal.novaraids.config.ConfigManager.GLOBAL_CONTRABAND_GUI;
import static me.unariginal.novaraids.utils.GuiUtils.openContrabandGui;
import static net.minecraft.server.command.CommandManager.literal;

public class RaidCheckGlobalBannedCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("global")
                .executes(RaidCheckGlobalBannedCommand::execute);
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        ContrabandGUIConfig guiConfig = GLOBAL_CONTRABAND_GUI;

        SimpleGui mainGui = new SimpleGui(guiConfig.getScreenHandler(), player, false);
        mainGui.setTitle(TextUtils.deserialize(TextUtils.parse(guiConfig.guiTitle)));

        List<Text> lore = new ArrayList<>();
        for (String line : guiConfig.bannedPokemon.itemLore) {
            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
        }
        for (Integer slot : guiConfig.getSlotsBySymbol(guiConfig.bannedPokemon.symbol)) {
            ItemStack item = guiConfig.bannedPokemon.item.copy();
            GuiElement element = new GuiElementBuilder(item)
                    .setName(TextUtils.deserialize(TextUtils.parse(guiConfig.bannedPokemon.itemName)))
                    .setLore(lore)
                    .setCallback(clickType -> {
                        mainGui.close();
                        openContrabandGui(player, guiConfig.bannedPokemon, "pokemon", null, null, 1);
                    })
                    .build();
            mainGui.setSlot(slot, element);
        }

        lore = new ArrayList<>();
        for (String line : guiConfig.bannedMoves.itemLore) {
            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
        }
        for (Integer slot : guiConfig.getSlotsBySymbol(guiConfig.bannedMoves.symbol)) {
            ItemStack item = guiConfig.bannedMoves.item.copy();
            GuiElement element = new GuiElementBuilder(item)
                    .setName(TextUtils.deserialize(TextUtils.parse(guiConfig.bannedMoves.itemName)))
                    .setLore(lore)
                    .setCallback(clickType -> {
                        mainGui.close();
                        openContrabandGui(player, guiConfig.bannedMoves, "move", null, null, 1);
                    })
                    .build();
            mainGui.setSlot(slot, element);
        }

        lore = new ArrayList<>();
        for (String line : guiConfig.bannedAbilities.itemLore) {
            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
        }
        for (Integer slot : guiConfig.getSlotsBySymbol(guiConfig.bannedAbilities.symbol)) {
            ItemStack item = guiConfig.bannedAbilities.item.copy();
            GuiElement element = new GuiElementBuilder(item)
                    .setName(TextUtils.deserialize(TextUtils.parse(guiConfig.bannedAbilities.itemName)))
                    .setLore(lore)
                    .setCallback(clickType -> {
                        mainGui.close();
                        openContrabandGui(player, guiConfig.bannedAbilities, "ability", null, null, 1);
                    })
                    .build();
            mainGui.setSlot(slot, element);
        }

        lore = new ArrayList<>();
        for (String line : guiConfig.bannedHeldItems.itemLore) {
            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
        }
        for (Integer slot : guiConfig.getSlotsBySymbol(guiConfig.bannedHeldItems.symbol)) {
            ItemStack item = guiConfig.bannedHeldItems.item.copy();
            GuiElement element = new GuiElementBuilder(item)
                    .setName(TextUtils.deserialize(TextUtils.parse(guiConfig.bannedHeldItems.itemName)))
                    .setLore(lore)
                    .setCallback(clickType -> {
                        mainGui.close();
                        openContrabandGui(player, guiConfig.bannedHeldItems, "held_item", null, null, 1);
                    })
                    .build();
            mainGui.setSlot(slot, element);
        }

        lore = new ArrayList<>();
        for (String line : guiConfig.bannedBagItems.itemLore) {
            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
        }
        for (Integer slot : guiConfig.getSlotsBySymbol(guiConfig.bannedBagItems.symbol)) {
            ItemStack item = guiConfig.bannedBagItems.item.copy();
            GuiElement element = new GuiElementBuilder(item)
                    .setName(TextUtils.deserialize(TextUtils.parse(guiConfig.bannedBagItems.itemName)))
                    .setLore(lore)
                    .setCallback(clickType -> {
                        mainGui.close();
                        openContrabandGui(player, guiConfig.bannedBagItems, "bag_item", null, null, 1);
                    })
                    .build();
            mainGui.setSlot(slot, element);
        }

        lore = new ArrayList<>();
        for (String line : guiConfig.backgroundItem.itemLore) {
            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
        }
        for (Integer slot : guiConfig.getSlotsBySymbol(guiConfig.backgroundItem.symbol)) {
            ItemStack item = guiConfig.backgroundItem.item.copy();
            GuiElement element = new GuiElementBuilder(item)
                    .setName(TextUtils.deserialize(TextUtils.parse(guiConfig.backgroundItem.itemName)))
                    .setLore(lore)
                    .setCallback(clickType -> mainGui.close())
                    .build();
            mainGui.setSlot(slot, element);
        }

        lore = new ArrayList<>();
        for (String line : guiConfig.closeItem.itemLore) {
            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
        }
        for (Integer slot : guiConfig.getSlotsBySymbol(guiConfig.closeItem.symbol)) {
            ItemStack item = guiConfig.closeItem.item.copy();
            GuiElement element = new GuiElementBuilder(item)
                    .setName(TextUtils.deserialize(TextUtils.parse(guiConfig.backgroundItem.itemName)))
                    .setLore(lore)
                    .setCallback(clickType -> mainGui.close())
                    .build();
            mainGui.setSlot(slot, element);
        }

        mainGui.open();
        return Command.SINGLE_SUCCESS;
    }
}
