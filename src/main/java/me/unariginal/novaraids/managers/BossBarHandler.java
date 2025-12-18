package me.unariginal.novaraids.managers;

import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.utils.Threading;
import net.kyori.adventure.bossbar.BossBar;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class BossBarHandler {
    public BossBarHandler() {
        Threading.runDelayedTaskAsyncTimer(this::updateBossBars, 2L, 2L);
    }

    public void updateBossBars() {
        Collection<Raid> raids = NovaRaids.INSTANCE.activeRaids().values();

        for (Raid raid : raids) {
            for (Map.Entry<UUID, BossBar> entry : raid.bossbars().entrySet()) {
                BossBar bossBar = entry.getValue();
                try {
                    if (raid.stage() == 2) {
                        float progress = Math.clamp((float) raid.currentHealth() / raid.maxHealth(), 0, 1);
                        bossBar.progress(progress);
                        continue;
                    }

                    float remainingTicks = (float) (raid.phaseEndTime() - NovaRaids.INSTANCE.server().getOverworld().getTime());
                    float progress = 1.0F / (raid.phaseLength() * 20L);
                    float total = Math.clamp(progress * remainingTicks, 0, 1);
                    bossBar.progress(total);
                } catch (IllegalArgumentException e) {
                    NovaRaids.LOGGER.error("[NovaRaids] Caught Bossbar Exception.", e);
                }
            }

            raid.showOverlay(raid.bossbarData());
        }
    }

}
