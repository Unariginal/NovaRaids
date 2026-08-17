package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.unariginal.novaraids.commands.suggestions.CategorySuggestions;
import me.unariginal.novaraids.data.categories.Category;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static me.unariginal.novaraids.config.ConfigManager.*;
import static me.unariginal.novaraids.guis.ContrabandGui.openContrabandGui;
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
        openContrabandGui(player, CATEGORY_CONTRABAND_GUI, category, null);
        return Command.SINGLE_SUCCESS;
    }
}
