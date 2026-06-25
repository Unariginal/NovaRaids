package me.unariginal.novaraids.config;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.*;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.placeholders.ParseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.*;
import java.util.*;

import static me.unariginal.novaraids.NovaRaids.logError;
import static me.unariginal.novaraids.utils.TextUtils.parse;

public class RewardPresetsConfig {
    public static class Reward {
        public transient String rewardId;
        public final transient UUID uuid = UUID.randomUUID();

        public void grantReward(ServerPlayerEntity player, Boss boss) {}
    }

    public static class ItemReward extends Reward {
        public ItemStack item;
        public int minCount;
        public int maxCount;

        @Override
        public void grantReward(ServerPlayerEntity player, Boss boss) {
            if (!player.giveItemStack(item.copyWithCount(new Random().nextInt(minCount, maxCount + 1)))) {
                player.getInventory().offerOrDrop(item.copyWithCount(new Random().nextInt(minCount, maxCount + 1)));
            }
        }
    }

    public static class CommandReward extends Reward {
        public List<String> commands;

        @Override
        public void grantReward(ServerPlayerEntity player, Boss boss) {
            CommandManager cmdManager = Objects.requireNonNull(player.getServer()).getCommandManager();
            ServerCommandSource source = player.getServer().getCommandSource();
            for (String command : commands) {
                cmdManager.executeWithPrefix(source, parse(command.replaceAll("%player%", player.getNameForScoreboard()), ParseContext.builder().player(player).boss(boss).build()));
            }
        }
    }

    public static class PokemonReward extends Reward {
        public PokemonProperties pokemon;

        @Override
        public void grantReward(ServerPlayerEntity player, Boss boss) {
            PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
            if (pokemon != null) party.add(pokemon.create(player));
            else logError("Pokemon was null!");
        }
    }

    public static Reward getReward(String id) {
        return ConfigManager.REWARD_PRESETS.get(id);
    }
}
