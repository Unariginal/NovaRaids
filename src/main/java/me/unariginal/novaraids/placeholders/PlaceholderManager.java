package me.unariginal.novaraids.placeholders;

import me.unariginal.novaraids.placeholders.interfaces.*;
import me.unariginal.novaraids.placeholders.services.MiniPlaceholdersService;
import me.unariginal.novaraids.placeholders.services.PlaceholderAPIService;
import me.unariginal.novaraids.placeholders.types.NovaRaidsPrefix;
import me.unariginal.novaraids.placeholders.types.boss.*;
import me.unariginal.novaraids.placeholders.types.categoryModifier.*;
import me.unariginal.novaraids.placeholders.types.history.*;
import me.unariginal.novaraids.placeholders.types.raid.*;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

public class PlaceholderManager {
    public static boolean usingMiniPlaceholders;
    public static boolean usingPlaceholderAPI;
    public static MiniPlaceholdersService miniPlaceholdersService;
    public static PlaceholderAPIService placeholderAPIService;

    public static final List<ServerPlaceholder> serverPlaceholders = List.of(
            new NovaRaidsPrefix()
    );

    public static final List<RaidPlaceholder> raidPlaceholders = List.of(
            new RaidCategory(),
            new RaidCategoryId(),
            new RaidCompletionTime(),
            new RaidDefeatedTime(),
            new RaidHealth(),
            new RaidJoinMethod(),
            new RaidLocation(),
            new RaidLocationId(),
            new RaidMaximumHealth(),
            new RaidMaximumLevel(),
            new RaidMaximumPartySize(),
            new RaidMaximumPlayers(),
            new RaidMinimumLevel(),
            new RaidMinimumPartySize(),
            new RaidMinimumPlayers(),
            new RaidParticipatingPlayers(),
            new RaidPhase(),
            new RaidPhaseTimer(),
            new RaidTimer(),
            new RaidTotalDamage(),
            new RaidUUID(),
            new RaidModifier(),
            new RaidModifierId(),
            new RaidId()
    );

    public static final List<BossPlaceholder> bossPlaceholders = List.of(
            new BossName(),
            new BossAbility(),
            new BossDynamaxLevel(),
            new BossEvs(),
            new BossForm(),
            new BossFriendship(),
            new BossGender(),
            new BossGmaxFactor(),
            new BossHeldItem(),
            new BossIvs(),
            new BossLevel(),
            new BossMoves(),
            new BossNature(),
            new BossScale(),
            new BossShiny(),
            new BossSpecies(),
            new BossTeraType()
    );

    public static final List<CategoryModifierPlaceholder> categoryModifierPlaceholders = List.of(
            new ModifierName(),
            new ModifierId(),
            new ModifierCatchLevelOffset(),
            new ModifierChance(),
            new ModifierDynamaxLevelOffset(),
            new ModifierEvs(),
            new ModifierHealthIncreaseOffset(),
            new ModifierHealthOffset(),
            new ModifierIvs(),
            new ModifierLevelOffset(),
            new ModifierMaximumLevelOffset(),
            new ModifierMaximumPartySizeOffset(),
            new ModifierMinimumLevelOffset(),
            new ModifierMinimumPartySizeOffset(),
            new ModifierScaleOffset(),
            new ModifierShinyOverride(),
            new ModifierSkillLevelOffset(),
            new ModifierWeight()
    );

    public static final List<RaidHistoryPlaceholder> raidHistoryPlaceholders = List.of(
            new HistoryBossAbility(),
            new HistoryBossEvs(),
            new HistoryBossDynamaxLevel(),
            new HistoryBossForm(),
            new HistoryBossGimmick(),
            new HistoryBossFriendship(),
            new HistoryBossGender(),
            new HistoryBossGmaxFactor(),
            new HistoryBossHeldItem(),
            new HistoryBossId(),
            new HistoryBossIvs(),
            new HistoryBossLevel(),
            new HistoryBossName(),
            new HistoryBossNature(),
            new HistoryBossScale(),
            new HistoryBossShiny(),
            new HistoryBossSpecies(),
            new HistoryBossTeraType(),
            new HistoryCategory(),
            new HistoryCategoryId(),
            new HistoryDuration(),
            new HistoryEndTicks(),
            new HistoryEndTime(),
            new HistoryFightDuration(),
            new HistoryFightEndTicks(),
            new HistoryFightStartTicks(),
            new HistoryHealth(),
            new HistoryLocation(),
            new HistoryLocationId(),
            new HistoryMaximumPlayers(),
            new HistoryMinimumPlayers(),
            new HistoryModifier(),
            new HistoryModifierId(),
            new HistoryPlayerCount(),
            new HistorySkillLevel(),
            new HistoryStartTicks(),
            new HistoryStartTime(),
            new HistoryStatus(),
            new HistoryUUID()
    );

    public static void registerPlaceholders() {
        usingMiniPlaceholders = FabricLoader.getInstance().isModLoaded("miniplaceholders");
        if (usingMiniPlaceholders) miniPlaceholdersService = new MiniPlaceholdersService();
        usingPlaceholderAPI = FabricLoader.getInstance().isModLoaded("placeholder-api");
        if (usingPlaceholderAPI) placeholderAPIService = new PlaceholderAPIService();

        serverPlaceholders.forEach(placeholder -> {
            if (usingMiniPlaceholders) miniPlaceholdersService.registerServer(placeholder);
            if (usingPlaceholderAPI) placeholderAPIService.registerServer(placeholder);
        });

        raidPlaceholders.forEach(placeholder -> {
            if (usingPlaceholderAPI) placeholderAPIService.registerRaid(placeholder);
        });

        bossPlaceholders.forEach(placeholder -> {
            if (usingPlaceholderAPI) placeholderAPIService.registerBoss(placeholder);
        });

        categoryModifierPlaceholders.forEach(placeholder -> {
            if (usingPlaceholderAPI) placeholderAPIService.registerCategoryModifier(placeholder);
        });

        raidHistoryPlaceholders.forEach(placeholder -> {
            if (usingMiniPlaceholders) miniPlaceholdersService.registerRaidHistory(placeholder);
            if (usingPlaceholderAPI) placeholderAPIService.registerRaidHistory(placeholder);
        });

        if (usingMiniPlaceholders) miniPlaceholdersService.registerBuilder();
    }
}
