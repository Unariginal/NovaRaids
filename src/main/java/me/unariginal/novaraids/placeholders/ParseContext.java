package me.unariginal.novaraids.placeholders;

import me.unariginal.novaraids.config.RaidHistory;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.data.categories.modifiers.CategoryModifier;
import me.unariginal.novaraids.data.players.PlayerRaidData;
import me.unariginal.novaraids.raid.Raid;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public class ParseContext {
    private final ServerPlayerEntity player;
    private final Raid raid;
    private final Boss boss;
    private final boolean prioritizeRaid;
    private final Category category;
    private final CategoryModifier modifier;
    private final RaidHistory raidHistory;
    private final PlayerRaidData playerRaidData;

    private ParseContext(Builder builder) {
        this.player = builder.player;
        this.raid = builder.raid;
        this.boss = builder.boss;
        this.prioritizeRaid = builder.prioritizeRaid;
        this.category = builder.category;
        this.modifier = builder.modifier;
        this.raidHistory = builder.raidHistory;
        this.playerRaidData = builder.playerRaidData;
    }

    @Nullable
    public ServerPlayerEntity getPlayer() {
        return player;
    }

    @Nullable
    public Raid getRaid() {
        return raid;
    }

    @Nullable
    public Boss getBoss() {
        return boss;
    }

    public boolean prioritizeRaid() {
        return prioritizeRaid;
    }

    @Nullable
    public Category getCategory() {
        return category;
    }

    @Nullable
    public CategoryModifier getModifier() {
        return modifier;
    }

    @Nullable
    public RaidHistory getRaidHistory() {
        return raidHistory;
    }

    @Nullable
    public PlayerRaidData getPlayerRaidData() {
        return playerRaidData;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ServerPlayerEntity player;
        private Raid raid;
        private Boss boss;
        private boolean prioritizeRaid = true;
        private Category category;
        private CategoryModifier modifier;
        private RaidHistory raidHistory;
        private PlayerRaidData playerRaidData;

        public Builder player(ServerPlayerEntity player) {
            this.player = player;
            return this;
        }

        public Builder raid(Raid raid) {
            this.raid = raid;
            return this;
        }

        public Builder boss(Boss boss) {
            this.boss = boss;
            return this;
        }

        public Builder prioritizeRaid(boolean prioritizeRaid) {
            this.prioritizeRaid = prioritizeRaid;
            return this;
        }

        public Builder category(Category category) {
            this.category = category;
            return this;
        }

        public Builder modifier(CategoryModifier modifier) {
            this.modifier = modifier;
            return this;
        }

        public Builder raidHistory(RaidHistory raidHistory) {
            this.raidHistory = raidHistory;
            return this;
        }

        public Builder playerRaidData(PlayerRaidData playerRaidData) {
            this.playerRaidData = playerRaidData;
            return this;
        }

        public ParseContext build() {
            return new ParseContext(this);
        }
    }
}
