package me.unariginal.novaraids.config;

import com.google.gson.*;
import me.unariginal.novaraids.data.Contraband;
import me.unariginal.novaraids.data.items.Pass;
import me.unariginal.novaraids.data.items.RaidBall;
import me.unariginal.novaraids.data.items.Voucher;

import java.io.*;
import java.util.*;

public class Config {
    public boolean debug = false;
    public boolean useExperimentalSharedBattles = false;
    public RaidSettings raidSettings = new RaidSettings();
    public ItemSettings itemSettings = new ItemSettings();
    public DiscordSettings discordWebhook = new DiscordSettings();

    // Raid Settings
    public static class RaidSettings {
        public boolean useQueueSystem = false;
        public boolean runRaidsWithNoPlayers = false;
        public boolean hideOtherCatchEncounters = true;
        public boolean hideOtherPlayersInRaid = false;
        public boolean hideOtherPokemonInRaid = false;
        public boolean reduceLargePokemonSize = true;
        public boolean disableSpawnsInArena = true;
        public boolean bossesHaveInfinitePP = false;
        public boolean allowExperienceGain = false;
        public boolean automaticBattles = false;
        public int automaticBattleDelay = 2;
        public Contraband globalContraband;
    }

    // Item Settings
    public static class ItemSettings {
        public VoucherSettings voucherSettings = new VoucherSettings();
        public PassSettings passSettings = new PassSettings();
        public RaidBallSettings raidBallSettings = new RaidBallSettings();

        public static class VoucherSettings {
            public boolean vouchersEnabled = true;
            public boolean vouchersJoinRaids = false;
            public Voucher defaultVoucher = new Voucher();
            public Voucher globalChoiceVoucher = new Voucher();
            public Voucher globalRandomVoucher = new Voucher();
        }

        public static class PassSettings {
            public boolean passesEnabled = true;
            public Pass defaultPass = new Pass();
            public Pass globalPass = new Pass();
        }

        public static class RaidBallSettings {
            public boolean raidBallsEnabled = true;
            public boolean playerLinkedRaidBalls = true;
            public Map<String, RaidBall> raidBalls = Map.of("default", new RaidBall());
        }
    }

    public static class DiscordSettings {
        public boolean enabled = false;
        public String url = "https://discord.com/api/webhooks/your_webhook_url";
        public String username = "Raid Alert!";
        public String avatarUrl = "https://cdn.modrinth.com/data/MdwFAVRL/e54083a07bcd9436d1f8d2879b0d821a54588b9e.png";
        public String thumbnailDatabaseUrl = "https://play.pokemonshowdown.com/sprites/%rute%/%pokemon%%form%.gif";
        public int updateRateSeconds = 15;
        public boolean deleteIfNoFightPhase = true;
        public List<String> blacklistedCategories;
        public List<String> blacklistedBosses;
    }

    public RaidBall getRaidBall(String id) {
        for (Map.Entry<String, RaidBall> raidBallEntry : itemSettings.raidBallSettings.raidBalls.entrySet()) {
            if (raidBallEntry.getKey().equals(id)) {
                return raidBallEntry.getValue();
            }
        }
        return null;
    }
}
