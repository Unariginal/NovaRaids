package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.commands.suggestions.BossSuggestions;
import me.unariginal.novaraids.config.RewardPoolsConfig;
import me.unariginal.novaraids.config.RewardPresetsConfig;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.data.categories.modifiers.CategoryModifier;
import me.unariginal.novaraids.data.rewards.DistributionSection;
import me.unariginal.novaraids.data.rewards.Place;
import me.unariginal.novaraids.data.rewards.RewardDistribution;
import me.unariginal.novaraids.raid.Raid;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.unariginal.novaraids.NovaRaids.logError;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RaidTestRewardsCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("testrewards")
                .requires(Permissions.require("novaraids.testrewards", 4))
                .then(argument("boss", StringArgumentType.string())
                        .suggests(new BossSuggestions())
                        .then(argument("modifier", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    String bossId = StringArgumentType.getString(ctx, "boss");
                                    Boss boss = Boss.getBoss(bossId);
                                    if (boss == null) return builder.buildFuture();
                                    Category category = Category.getCategory(boss.categoryId);
                                    if (category == null) return builder.buildFuture();
                                    category.modifiers.keySet().forEach(builder::suggest);
                                    builder.suggest("no_modifier");
                                    return builder.buildFuture();
                                })
                                .then(argument("placement", IntegerArgumentType.integer())
                                        .then(argument("total_players", IntegerArgumentType.integer())
                                                .executes(ctx -> execute(
                                                        ctx.getSource().getPlayer(),
                                                        Boss.getBoss(StringArgumentType.getString(ctx, "boss")),
                                                        CategoryModifier.getModifier(StringArgumentType.getString(ctx, "modifier")),
                                                        IntegerArgumentType.getInteger(ctx, "placement"),
                                                        IntegerArgumentType.getInteger(ctx, "total_players")
                                                ))))));
    }

    private static int execute(ServerPlayerEntity player, Boss boss, @Nullable CategoryModifier modifier, int placement, int totalPlayers) {
        if (player == null) return 0;
        Category category = Category.getCategory(boss.categoryId);
        if (category == null) return 0;

        List<RewardDistribution> rewards = Raid.getRewardDistribution(boss, category, modifier);

        Map<String, Integer> placementCount = new HashMap<>();
        for (RewardDistribution reward : rewards) {
            for (Place place : reward.places) {
                placementCount.put(
                        place.place.toLowerCase(),
                        placementCount.containsKey(place.place.toLowerCase()) ? placementCount.get(place.place.toLowerCase()) + 1 : 1
                );
            }
        }

        Map<UUID, String> rewardedPlayers = new HashMap<>();
        for (RewardDistribution reward : rewards) {
            for (Place place : reward.places) {
                List<UUID> playersToReward = new ArrayList<>();

                if (StringUtils.isNumeric(place.place)) {
                    int placeInt = Integer.parseInt(place.place);
                    if (placement == placeInt) {
                        playersToReward.add(player.getUuid());
                    }
                } else if (place.place.contains("%")) {
                    String percentStr = place.place.replace("%", "");
                    if (StringUtils.isNumeric(percentStr)) {
                        int percent = Integer.parseInt(percentStr);
                        double positions = totalPlayers * ((double) percent / 100);
                        for (int i = 1; i <= ((int) Math.ceil(positions)); i++) {
                            if (placement == i) {
                                playersToReward.add(player.getUuid());
                            }
                        }
                    } else logError("Invalid percentage in placement! " + place.place);
                } else if (place.place.equalsIgnoreCase("participating")) {
                    playersToReward.add(player.getUuid());
                }

                for (UUID playerUuid : playersToReward) {
                    ServerPlayerEntity serverPlayer = NovaRaids.INSTANCE.server.getPlayerManager().getPlayer(playerUuid);
                    if (serverPlayer == null) continue;

                    boolean duplicatePlacementExists = placementCount.get(place.place.toLowerCase()) > 1;
                    if (!rewardedPlayers.containsKey(playerUuid) || (duplicatePlacementExists && place.place.equalsIgnoreCase(rewardedPlayers.get(playerUuid)))) {
                        int rolls = new Random().nextInt(reward.rewards.minRolls, reward.rewards.maxRolls + 1);

                        List<UUID> distributedPools = new ArrayList<>();
                        for (int i = 0; i < rolls; i++) {
                            DistributionSection.RewardPoolSection poolSection = reward.rewards.getRandomRewardPool();
                            if (poolSection != null) {
                                RewardPoolsConfig.RewardPool pool = null;
                                if (poolSection instanceof DistributionSection.PredefinedRewardPoolSection predefinedPoolSection) {
                                    pool = RewardPoolsConfig.getRewardPool(predefinedPoolSection.poolPreset);
                                } else if (poolSection instanceof DistributionSection.UndefinedRewardPoolSection undefinedPoolSection) {
                                    pool = undefinedPoolSection.pool;
                                }

                                if (pool == null) {
                                    logError("RewardPool was null!");
                                    continue;
                                }

                                if (reward.rewards.allowDuplicates || !distributedPools.contains(pool.uuid)) {
                                    List<RewardPresetsConfig.Reward> distributionList = pool.distributeRewards();
                                    distributionList.forEach(distributionItem -> distributionItem.grantReward(serverPlayer));
                                    distributedPools.add(pool.uuid);
                                } else i--;
                            } else logError("RewardPoolSection was null!");
                        }
                    }
                }

                for (UUID playerUuid : playersToReward) {
                    if (!place.allowOtherRewards && !rewardedPlayers.containsKey(playerUuid)) {
                        rewardedPlayers.put(playerUuid, place.place);
                    }
                }
            }
        }
        return Command.SINGLE_SUCCESS;
    }
}
