package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.unariginal.novaraids.commands.suggestions.CategorySuggestions;
import me.unariginal.novaraids.config.guis.ContrabandGUIConfig;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.utils.TextUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

import static me.unariginal.novaraids.config.ConfigManager.*;
import static me.unariginal.novaraids.utils.GuiUtils.openContrabandGui;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RaidCheckCategoryBannedCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("category")
                .then(argument("category", StringArgumentType.string())
                        .suggests(new CategorySuggestions())
                        .executes(RaidCheckCategoryBannedCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        Category category = Category.getCategory(StringArgumentType.getString(ctx, "category"));
        if (category == null) return 0;

        ContrabandGUIConfig guiConfig = CATEGORY_CONTRABAND_GUI;

        SimpleGui mainGui = new SimpleGui(guiConfig.getScreenHandler(), player, false);
        mainGui.setTitle(TextUtils.deserialize(TextUtils.parse(guiConfig.guiTitle.replaceAll("%category%", category.categoryName))));

        List<Text> lore = new ArrayList<>();
        for (String line : guiConfig.bannedPokemon.itemLore) {
            lore.add(TextUtils.deserialize(TextUtils.parse(line.replaceAll("%category%", category.categoryName))));
        }
        for (Integer slot : guiConfig.getSlotsBySymbol(guiConfig.bannedPokemon.symbol)) {
            ItemStack item = guiConfig.bannedPokemon.item.copy();
            GuiElement element = new GuiElementBuilder(item)
                    .setName(TextUtils.deserialize(TextUtils.parse(guiConfig.bannedPokemon.itemName.replaceAll("%category%", category.categoryName))))
                    .setLore(lore)
                    .setCallback(clickType -> {
                        mainGui.close();
                        openContrabandGui(player, guiConfig.bannedPokemon, "pokemon", category, null, 1);
                    })
                    .build();
            mainGui.setSlot(slot, element);
        }

        lore = new ArrayList<>();
        for (String line : guiConfig.bannedMoves.itemLore) {
            lore.add(TextUtils.deserialize(TextUtils.parse(line.replaceAll("%category%", category.categoryName))));
        }
        for (Integer slot : guiConfig.getSlotsBySymbol(guiConfig.bannedMoves.symbol)) {
            ItemStack item = guiConfig.bannedMoves.item.copy();
            GuiElement element = new GuiElementBuilder(item)
                    .setName(TextUtils.deserialize(TextUtils.parse(guiConfig.bannedMoves.itemName.replaceAll("%category%", category.categoryName))))
                    .setLore(lore)
                    .setCallback(clickType -> {
                        mainGui.close();
                        openContrabandGui(player, guiConfig.bannedMoves, "move", category, null, 1);
                    })
                    .build();
            mainGui.setSlot(slot, element);
        }

        lore = new ArrayList<>();
        for (String line : guiConfig.bannedAbilities.itemLore) {
            lore.add(TextUtils.deserialize(TextUtils.parse(line.replaceAll("%category%", category.categoryName))));
        }
        for (Integer slot : guiConfig.getSlotsBySymbol(guiConfig.bannedAbilities.symbol)) {
            ItemStack item = guiConfig.bannedAbilities.item.copy();
            GuiElement element = new GuiElementBuilder(item)
                    .setName(TextUtils.deserialize(TextUtils.parse(guiConfig.bannedAbilities.itemName.replaceAll("%category%", category.categoryName))))
                    .setLore(lore)
                    .setCallback(clickType -> {
                        mainGui.close();
                        openContrabandGui(player, guiConfig.bannedAbilities, "ability", category, null, 1);
                    })
                    .build();
            mainGui.setSlot(slot, element);
        }

        lore = new ArrayList<>();
        for (String line : guiConfig.bannedHeldItems.itemLore) {
            lore.add(TextUtils.deserialize(TextUtils.parse(line.replaceAll("%category%", category.categoryName))));
        }
        for (Integer slot : guiConfig.getSlotsBySymbol(guiConfig.bannedHeldItems.symbol)) {
            ItemStack item = guiConfig.bannedHeldItems.item.copy();
            GuiElement element = new GuiElementBuilder(item)
                    .setName(TextUtils.deserialize(TextUtils.parse(guiConfig.bannedHeldItems.itemName.replaceAll("%category%", category.categoryName))))
                    .setLore(lore)
                    .setCallback(clickType -> {
                        mainGui.close();
                        openContrabandGui(player, guiConfig.bannedHeldItems, "held_item", category, null, 1);
                    })
                    .build();
            mainGui.setSlot(slot, element);
        }

        lore = new ArrayList<>();
        for (String line : guiConfig.bannedBagItems.itemLore) {
            lore.add(TextUtils.deserialize(TextUtils.parse(line.replaceAll("%category%", category.categoryName))));
        }
        for (Integer slot : guiConfig.getSlotsBySymbol(guiConfig.bannedBagItems.symbol)) {
            ItemStack item = guiConfig.bannedBagItems.item.copy();
            GuiElement element = new GuiElementBuilder(item)
                    .setName(TextUtils.deserialize(TextUtils.parse(guiConfig.bannedBagItems.itemName.replaceAll("%category%", category.categoryName))))
                    .setLore(lore)
                    .setCallback(clickType -> {
                        mainGui.close();
                        openContrabandGui(player, guiConfig.bannedBagItems, "bag_item", category, null, 1);
                    })
                    .build();
            mainGui.setSlot(slot, element);
        }

        lore = new ArrayList<>();
        for (String line : guiConfig.backgroundItem.itemLore) {
            lore.add(TextUtils.deserialize(TextUtils.parse(line.replaceAll("%category%", category.categoryName))));
        }
        for (Integer slot : guiConfig.getSlotsBySymbol(guiConfig.backgroundItem.symbol)) {
            ItemStack item = guiConfig.backgroundItem.item.copy();
            GuiElement element = new GuiElementBuilder(item)
                    .setName(TextUtils.deserialize(TextUtils.parse(guiConfig.backgroundItem.itemName.replaceAll("%category%", category.categoryName))))
                    .setLore(lore)
                    .setCallback(clickType -> mainGui.close())
                    .build();
            mainGui.setSlot(slot, element);
        }

        lore = new ArrayList<>();
        for (String line : guiConfig.closeItem.itemLore) {
            lore.add(TextUtils.deserialize(TextUtils.parse(line.replaceAll("%category%", category.categoryName))));
        }
        for (Integer slot : guiConfig.getSlotsBySymbol(guiConfig.closeItem.symbol)) {
            ItemStack item = guiConfig.closeItem.item.copy();
            GuiElement element = new GuiElementBuilder(item)
                    .setName(TextUtils.deserialize(TextUtils.parse(guiConfig.backgroundItem.itemName.replaceAll("%category%", category.categoryName))))
                    .setLore(lore)
                    .setCallback(clickType -> mainGui.close())
                    .build();
            mainGui.setSlot(slot, element);
        }

        mainGui.open();
        return Command.SINGLE_SUCCESS;
    }
}
