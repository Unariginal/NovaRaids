package me.unariginal.novaraids.managers;

import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.utils.Threading;
import net.kyori.adventure.bossbar.BossBar;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class BossBarHandler {

    private final NovaRaids nr;

    public BossBarHandler(NovaRaids plugin) {
        this.nr = plugin;

        Threading.runDelayedTaskAsyncTimer(this::updateBossBars, 3L, 3L);
    }

    public void updateBossBars() {
        Collection<Raid> raids = nr.activeRaids().values();

        for (Raid raid : raids) {
            for(Map.Entry<UUID, BossBar> entry : raid.bossbars().entrySet()) {

                BossBar bossBar = entry.getValue();

                if(raid.stage() == 2) {
                    float progress = Math.clamp((float) raid.currentHealth() / raid.maxHealth(), 0, 1);
                    bossBar.progress(progress);
                    continue;
                }

                float remainingTicks = (float) (raid.phaseEndTime() - nr.server().getOverworld().getTime());
                float progress = 1.0F / (raid.phaseLength() * 20L);
                float total = Math.clamp(progress * remainingTicks, 0, 1);
                bossBar.progress(total);

            }

            raid.showOverlay(raid.bossbarData());
        }
    }

}
