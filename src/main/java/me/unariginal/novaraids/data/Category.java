package me.unariginal.novaraids.data;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.pokemon.Species;
import me.unariginal.novaraids.data.items.Pass;
import me.unariginal.novaraids.data.items.RaidBall;
import me.unariginal.novaraids.data.items.Voucher;
import me.unariginal.novaraids.data.rewards.DistributionSection;
import net.minecraft.item.Item;

import java.util.List;

public record Category(String id,
                       String name,
                       boolean requirePass,
                       int minPlayers,
                       int maxPlayers,
                       List<Species> bannedPokemon,
                       List<Move> bannedMoves,
                       List<Ability> bannedAbilities,
                       List<Item> bannedHeldItems,
                       List<Item> bannedBagItems,
                       String setupBossbar,
                       String fightBossbar,
                       String preCatchBossbar,
                       String catchBossbar,
                       Voucher categoryChoiceVoucher,
                       Voucher categoryRandomVoucher,
                       Pass categoryPass,
                       List<RaidBall> categoryBalls,
                       List<DistributionSection> rewards) {
    public RaidBall getRaidBall(String key) {
        for (RaidBall ball : categoryBalls) {
            if (ball.id().equals(key)) {
                return ball;
            }
        }
        return null;
    }
}
