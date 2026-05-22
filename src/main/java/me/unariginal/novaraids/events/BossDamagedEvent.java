package me.unariginal.novaraids.events;

import me.unariginal.novaraids.raid.Raid;
import net.minecraft.util.ActionResult;

public interface BossDamagedEvent {
    interface Pre {
        ActionResult onBossDamagedPre(Raid raid, int damage);
    }
    interface Post {
        ActionResult onBossDamagedPost(Raid raid, int damage);
    }
}
