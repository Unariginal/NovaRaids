package me.unariginal.novaraids.data.bosssettings;

import com.cobblemon.mod.common.pokemon.Species;

import java.util.List;

public record CatchSettings(Species speciesOverride,
                            int levelOverride,
                            boolean keepFeatures,
                            String featuresOverride,
                            boolean keepScale,
                            boolean keepHeldItem,
                            boolean randomizeIvs,
                            boolean keepEvs,
                            boolean randomizeGender,
                            boolean randomizeNature,
                            boolean randomizeAbility,
                            boolean resetMoves,
                            int friendshipOverride,
                            boolean randomizeTeraType,
                            boolean resetGmaxFactor,
                            int dmaxLevelOverride,
                            List<CatchPlacement> catchPlacements) {}
