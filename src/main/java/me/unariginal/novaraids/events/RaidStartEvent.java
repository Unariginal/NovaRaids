package me.unariginal.novaraids.events;

import me.unariginal.novaraids.raid.Raid;
import net.minecraft.util.ActionResult;

public interface RaidStartEvent {
    interface Pre {
        ActionResult onRaidStartPre(Raid raid);
    }
    interface Post {
        ActionResult onRaidStartPost(Raid raid);
    }
}
