package me.unariginal.novaraids.config;

import com.cobblemon.mod.common.pokemon.EVs;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.IVs;
import me.unariginal.novaraids.data.players.PlayerRaidData;
import me.unariginal.novaraids.raid.RaidStatus;
import net.minecraft.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RaidHistory {
    public String uuid;
    public RaidStatus raidStatus;//
    public String realStartTime;
    public String realEndTime;
    public String categoryId;
    public String categoryName;//
    public String modifierId;
    public String modifierName;//
    public String locationId;
    public String locationName;
    public int aiSkillLevel;
    public int minPlayers;
    public int maxPlayers;
    public int maxHealth;//
    public long startTime;//
    public long endTime;//
    public long fightStartTime;
    public long fightEndTime;
    public BossInformation boss;//
    public LinkedHashMap<String, Integer> damageLeaderboard;//
    public Map<String, PlayerRaidData> playerRaidData;

    public RaidHistory(
            String uuid,
            RaidStatus raidStatus,
            String realStartTime,
            String realEndTime,
            String categoryId,
            String categoryName,
            String modifierId,
            String modifierName,
            String locationId,
            String locationName,
            int aiSkillLevel,
            int minPlayers,
            int maxPlayers,
            int maxHealth,
            long startTime,
            long endTime,
            long fightStartTime,
            long fightEndTime,
            BossInformation boss,
            LinkedHashMap<String, Integer> damageLeaderboard,
            Map<String, PlayerRaidData> playerRaidData
    ) {
        this.uuid = uuid;
        this.raidStatus = raidStatus;
        this.realStartTime = realStartTime;
        this.realEndTime = realEndTime;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.modifierId = modifierId;
        this.modifierName = modifierName;
        this.locationId = locationId;
        this.locationName = locationName;
        this.aiSkillLevel = aiSkillLevel;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.maxHealth = maxHealth;
        this.startTime = startTime;
        this.endTime = endTime;
        this.fightStartTime = fightStartTime;
        this.fightEndTime = fightEndTime;
        this.boss = boss;
        this.damageLeaderboard = damageLeaderboard;
        this.playerRaidData = playerRaidData;
    }

    public static class BossInformation {
        public String bossId;
        public String displayName;//
        public String species;
        public int level;//
        public String formId;
        public String ability;
        public String nature;
        public Gender gender;
        public boolean shiny;
        public String gimmick;
        public String teraType;
        public boolean gmaxFactor;
        public int dynamaxLevel;
        public float scale;
        public ItemStack heldItem;
        public List<String> moves;
        public int friendship;
        public IVs ivs;
        public EVs evs;

        public BossInformation(
                String bossId,
                String displayName,
                String species,
                int level,
                String formId,
                String ability,
                String nature,
                Gender gender,
                boolean shiny,
                String gimmick,
                String teraType,
                boolean gmaxFactor,
                int dynamaxLevel,
                float scale,
                ItemStack heldItem,
                List<String> moves,
                int friendship,
                IVs ivs,
                EVs evs
        ) {
            this.bossId = bossId;
            this.displayName = displayName;
            this.species = species;
            this.level = level;
            this.formId = formId;
            this.ability = ability;
            this.nature = nature;
            this.gender = gender;
            this.shiny = shiny;
            this.gimmick = gimmick;
            this.teraType = teraType;
            this.gmaxFactor = gmaxFactor;
            this.dynamaxLevel = dynamaxLevel;
            this.scale = scale;
            this.heldItem = heldItem;
            this.moves = moves;
            this.friendship = friendship;
            this.ivs = ivs;
            this.evs = evs;
        }
    }
}
