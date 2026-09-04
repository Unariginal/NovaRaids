package me.unariginal.novaraids.commands.command;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.commands.suggestions.BossSuggestions;
import me.unariginal.novaraids.commands.suggestions.CategorySuggestions;
import me.unariginal.novaraids.config.ConfigManager;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static me.unariginal.novaraids.config.ConfigManager.BOSSES;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RaidGenerateStatsCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("generatestats")
                .requires(Permissions.require("novaraids.generatestats", 4))
                .then(literal("category")
                        .then(argument("category", StringArgumentType.string())
                                .suggests(new CategorySuggestions())
                                .then(argument("basemulti", IntegerArgumentType.integer(1))
                                        .then(argument("playermulti", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    String categoryId = StringArgumentType.getString(ctx, "category");
                                                    Integer baseMulti = IntegerArgumentType.getInteger(ctx, "basemulti");
                                                    Integer playerMulti = IntegerArgumentType.getInteger(ctx, "playermulti");
                                                    return execute(categoryId, null, baseMulti, playerMulti);
                                                })))
                                .executes(ctx -> {
                                    String categoryId = StringArgumentType.getString(ctx, "category");
                                    return execute(categoryId, null, null, null);
                                })))
                .then(literal("boss")
                        .then(argument("boss", StringArgumentType.string())
                                .suggests(new BossSuggestions())
                                .then(argument("basemulti", IntegerArgumentType.integer(1))
                                        .then(argument("playermulti", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    String bossId = StringArgumentType.getString(ctx, "boss");
                                                    Integer baseMulti = IntegerArgumentType.getInteger(ctx, "basemulti");
                                                    Integer playerMulti = IntegerArgumentType.getInteger(ctx, "playermulti");
                                                    return execute(null, bossId, baseMulti, playerMulti);
                                                })))
                                .executes(ctx -> {
                                    String bossId = StringArgumentType.getString(ctx, "boss");
                                    return execute(null, bossId, null, null);
                                })))
                .then(literal("all")
                        .then(argument("basemulti", IntegerArgumentType.integer(1))
                                .then(argument("playermulti", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            Integer baseMulti = IntegerArgumentType.getInteger(ctx, "basemulti");
                                            Integer playerMulti = IntegerArgumentType.getInteger(ctx, "playermulti");
                                            return execute(null, null, baseMulti, playerMulti);
                                        })))
                        .executes(ctx -> execute(null, null, null, null)));
    }

    private static int execute(@Nullable String categoryId, @Nullable String bossId, @Nullable Integer baseMulti, @Nullable Integer playerMulti) {
        Category category = Category.getCategory(categoryId);
        Boss boss = Boss.getBoss(bossId);

        List<Boss> bosses = new ArrayList<>();
        if (category != null) bosses.addAll(category.bosses.values());
        else if (boss != null) bosses.add(boss);
        else bosses.addAll(BOSSES.values());

        for (Boss loopBoss : bosses) {
            Pokemon pokemon = loopBoss.pokemonDetails.createPokemon(null);
            int health = pokemon.getMaxHealth();
            int baseHealth = health * (baseMulti == null ? 10 : baseMulti);
            int perPlayer = health * (playerMulti == null ? 5 : playerMulti);
            int atk = pokemon.getAttack();
            int def = pokemon.getDefence();
            int spa = pokemon.getSpecialAttack();
            int spd = pokemon.getSpecialDefence();
            int spe = pokemon.getSpeed();

            String output = "{\n" +
                    "  \"boss_id\": \"" + loopBoss.bossId + "\"," +
                    "\n  \"category_id\": \"" + loopBoss.categoryId + "\"," +
                    "\n  \"pokemon_health\": " + health + "," +
                    "\n  \"recommended_base_health\": " + baseHealth + "," +
                    "\n  \"recommended_per_player\": " + perPlayer + "," +
                    "\n  \"atk\": " + atk + "," +
                    "\n  \"def\": " + def + "," +
                    "\n  \"spa\": " + spa + "," +
                    "\n  \"spd\": " + spd + "," +
                    "\n  \"spe\": " + spe +
                    "\n}";

            File file = new File(ConfigManager.configDir, "/generated/health/" + loopBoss.categoryId + "/" + loopBoss.bossId + ".json");
            file.getParentFile().mkdirs();
            ConfigManager.writeFile(file, output);
        }
        return Command.SINGLE_SUCCESS;
    }
}
