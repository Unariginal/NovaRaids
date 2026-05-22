package me.unariginal.novaraids.events;

import me.unariginal.novaraids.raid.Raid;
import net.minecraft.util.ActionResult;

public interface BossDefeatedEvent {
    interface Pre {
        ActionResult onBossDefeatedPre(Raid raid);
    }
    interface Post {
        ActionResult onBossDefeatedPost(Raid raid);
    }
}
