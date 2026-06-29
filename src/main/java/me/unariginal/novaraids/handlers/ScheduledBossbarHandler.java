package me.unariginal.novaraids.handlers;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.raid.RaidPhase;
import me.unariginal.novaraids.utils.GlowUtils;
import me.unariginal.novaraids.utils.Threading;
import net.kyori.adventure.bossbar.BossBar;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

import static me.unariginal.novaraids.NovaRaids.logInfo;
import static me.unariginal.novaraids.raid.RaidManager.activeRaids;

public class ScheduledBossbarHandler {
    public ScheduledFuture<?> schedule;

    public ScheduledBossbarHandler() {
        schedule = Threading.runDelayedTaskAsyncTimer(this::updateBossBars, 2L, 2L);
    }

    public void updateBossBars() {
        Collection<Raid> raids = activeRaids.values();

        for (Raid raid : raids) {
            for (Map.Entry<UUID, BossBar> entry : raid.playerBossbars.entrySet()) {
                BossBar bossBar = entry.getValue();
                try {
                    if (raid.phase == RaidPhase.FIGHT) {
                        float progress = Math.clamp((float) raid.currentHealth / raid.maxHealth, 0, 1);
                        bossBar.progress(progress);
                        continue;
                    }

                    float remainingTicks = (float) (raid.phaseEndTime() - raid.location.getServerWorld().getTime());
                    float progress = 1.0F / (raid.phaseLength * 20L);
                    float total = Math.clamp(progress * remainingTicks, 0, 1);
                    bossBar.progress(total);
                } catch (IllegalArgumentException e) {
                    logInfo("Caught Bossbar Exception.");
                }
            }

            raid.showOverlay(raid.bossbarData);

            NovaRaids.INSTANCE.server.execute(() -> {
                PokemonEntity bossEntity = raid.getBossEntity();
                if (bossEntity == null) return;

                if ((raid.modifier != null && raid.modifier.bossDetailModifiers.glowingOverride)
                        || raid.boss.bossDetails.applyGlowing) {

                    var color = raid.modifier != null
                            && raid.modifier.bossDetailModifiers.glowColorOverrideToggle
                            ? raid.modifier.bossDetailModifiers.glowColorOverride
                            : raid.boss.bossDetails.glowColor;

                    GlowUtils.applyGlowing(color, raid.bossPokemonUncatchable, bossEntity);
                }
            });
        }
    }
}
