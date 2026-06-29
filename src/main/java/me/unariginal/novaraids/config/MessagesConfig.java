package me.unariginal.novaraids.config;

public class MessagesConfig {
    public String prefix = "<dark_gray>[</dark_gray><color:#ffbf00>RAID<dark_gray>]</dark_gray>";
    public String notEnoughPlayers;
    public String noAvailableLocations;
    public CommandMessages commands;
    public FeedbackMessages feedback;
    public LeaderboardMessages leaderboard;

    public static class CommandMessages {
        public String reload;
        public String raidStopped;
        public String giveInvalidCategory;
        public String giveInvalidBoss;
        public String giveInvalidPokeball;
        public String giveFailedToGive;
        public String giveReceivedItem;
        public String giveFeedback;
        public String checkbannedNoBannedPokemon;
        public String checkbannedNoBannedMoves;
        public String checkbannedNoBannedAbilities;
        public String checkbannedNoBannedHeldItems;
        public String checkbannedNoBannedBagItems;
    }

    public static class FeedbackMessages {
        public String joinedRaid;
        public String noActiveRaids;
        public String noQueuedRaids;
        public String usedVoucher;
        public String queueItemCancelled;
        public String addedToQueue;
        public WarningMessages warnings;

        public static class WarningMessages {
            public String noPass;
            public String alreadyInRaid;
            public String noPassNeeded;
            public String notJoinable;
            public String notEnoughPokemon;
            public String tooManyPokemon;
            public String maxPlayers;
            public String minimumLevel;
            public String maximumLevel;
            public String bannedPokemon;
            public String bannedMove;
            public String bannedAbility;
            public String bannedHeldItem;
            public String bannedBagItem;
            public String cooldown;
            public String battleDuringRaid;
            public String notYourEncounter;
            public String notYourRaidPokeball;
            public String raidPokeballOutsideRaid;
            public String notCatchPhase;
            public String normalPokeball;
            public String noBossesAvailable;
            public String bossDoesntExist;
            public String categoryDoesntExist;
        }
    }

    public static class LeaderboardMessages {
        public String header;
        public String placement;
        public String individual;
    }
}
